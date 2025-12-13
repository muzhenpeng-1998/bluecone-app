package com.bluecone.app.core.event.consume.application;

import com.bluecone.app.core.event.consume.api.ConsumeOptions;
import com.bluecone.app.core.event.consume.api.ConsumeResult;
import com.bluecone.app.core.event.consume.api.EventConsumeConflictException;
import com.bluecone.app.core.event.consume.api.EventConsumeFailedException;
import com.bluecone.app.core.event.consume.api.EventEnvelope;
import com.bluecone.app.core.event.consume.api.EventHandler;
import com.bluecone.app.core.event.consume.api.EventHandlerTemplate;
import com.bluecone.app.core.event.consume.spi.ConsumeMetrics;
import com.bluecone.app.core.event.consume.spi.EventDedupRepository;
import com.bluecone.app.core.event.consume.spi.EventDedupRepository.AcquireConsumeCommand;
import com.bluecone.app.core.event.consume.spi.EventDedupRepository.AcquireConsumeResult;
import com.bluecone.app.core.event.consume.spi.EventDedupRepository.AcquireConsumeState;
import com.bluecone.app.core.event.consume.spi.EventDedupRepository.ConsumeRecord;
import com.bluecone.app.core.event.consume.spi.EventDedupRepository.MarkConsumeFailedCommand;
import com.bluecone.app.core.event.consume.spi.EventDedupRepository.MarkConsumeSuccessCommand;
import com.bluecone.app.id.core.Ulid128;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * 默认事件消费模板实现，负责：
 * <ul>
 *     <li>基于 consumer_group + event_id 的消费幂等；</li>
 *     <li>多实例并发下的处理权抢占；</li>
 *     <li>失败重试与退避。</li>
 * </ul>
 */
public class DefaultEventHandlerTemplate implements EventHandlerTemplate {

    private static final int STATUS_PROCESSING = 0;
    private static final int STATUS_SUCCEEDED = 1;
    private static final int STATUS_FAILED = 2;

    private final EventDedupRepository repository;
    private final PlatformTransactionManager transactionManager;
    private final Clock clock;
    private final ConsumeMetrics metrics;

    public DefaultEventHandlerTemplate(EventDedupRepository repository,
                                       PlatformTransactionManager transactionManager,
                                       Clock clock,
                                       ConsumeMetrics metrics) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager must not be null");
        this.clock = (clock != null ? clock : Clock.systemUTC());
        this.metrics = (metrics != null ? metrics : ConsumeMetrics.noop());
    }

    @Override
    public ConsumeResult consume(String consumerGroup,
                                 EventEnvelope event,
                                 EventHandler handler,
                                 ConsumeOptions options) {
        Objects.requireNonNull(consumerGroup, "consumerGroup must not be null");
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        Objects.requireNonNull(options, "options must not be null");

        Instant now = Instant.now(clock);
        Instant lockUntil = now.plus(options.lockTtl());

        AcquireConsumeCommand command = new AcquireConsumeCommand(
                event.tenantId(),
                consumerGroup,
                event.eventId(),
                event.eventType(),
                now,
                lockUntil
        );

        AcquireConsumeResult acquireResult = repository.tryAcquire(command);
        ConsumeRecord record = acquireResult.record();

        return switch (acquireResult.state()) {
            case CONFLICT -> throw conflictException(consumerGroup, event.eventId(), event.eventType());
            case REPLAY_SUCCEEDED -> {
                metrics.onReplay();
                int retryCount = record != null ? record.retryCount() : 0;
                yield new ConsumeResult(true, false, true, retryCount);
            }
            case IN_PROGRESS -> handleInProgress(consumerGroup, event, options, record);
            case RETRYABLE_FAILED, ACQUIRED -> {
                metrics.onAcquire();
                yield executeInTx(consumerGroup, event, handler, options, record);
            }
        };
    }

    private EventConsumeConflictException conflictException(String consumerGroup,
                                                            Ulid128 eventId,
                                                            String eventType) {
        return new EventConsumeConflictException(
                "事件消费冲突：consumerGroup=" + consumerGroup
                        + ", eventId=" + eventId
                        + ", eventType=" + eventType);
    }

    private ConsumeResult handleInProgress(String consumerGroup,
                                           EventEnvelope event,
                                           ConsumeOptions options,
                                           ConsumeRecord currentRecord) {
        metrics.onInProgress();
        if (!options.waitIfInProgress() || options.waitMax() == null || options.waitMax().isZero()
                || options.waitMax().isNegative()) {
            int retryCount = currentRecord != null ? currentRecord.retryCount() : 0;
            return new ConsumeResult(false, true, false, retryCount);
        }

        Instant deadline = Instant.now(clock).plus(options.waitMax());
        int lastRetryCount = currentRecord != null ? currentRecord.retryCount() : 0;
        while (Instant.now(clock).isBefore(deadline)) {
            Optional<ConsumeRecord> opt = repository.find(consumerGroup, event.eventId());
            if (opt.isEmpty()) {
                break;
            }
            ConsumeRecord record = opt.get();
            lastRetryCount = record.retryCount();
            if (record.status() == STATUS_SUCCEEDED) {
                metrics.onReplay();
                return new ConsumeResult(true, false, true, lastRetryCount);
            }
            if (record.status() == STATUS_FAILED) {
                throw new EventConsumeFailedException(
                        "事件消费失败：consumerGroup=" + consumerGroup
                                + ", eventId=" + event.eventId()
                                + ", eventType=" + event.eventType());
            }
            sleepQuietly(50);
        }
        return new ConsumeResult(false, true, false, lastRetryCount);
    }

    private ConsumeResult executeInTx(String consumerGroup,
                                      EventEnvelope event,
                                      EventHandler handler,
                                      ConsumeOptions options,
                                      ConsumeRecord record) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        try {
            template.execute(status -> {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    // 在事务中标记失败并重新抛出
                    markFailed(consumerGroup, event, options, record, e);
                    throw new RuntimeException(e);
                }
                markSuccess(consumerGroup, event);
                return null;
            });
            metrics.onSuccess();
            int retryCount = record != null ? record.retryCount() : 0;
            return new ConsumeResult(false, false, true, retryCount);
        } catch (RuntimeException ex) {
            metrics.onFailure();
            // 原因可能被包装在 RuntimeException 中
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw ex;
        }
    }

    private void markSuccess(String consumerGroup, EventEnvelope event) {
        Instant processedAt = Instant.now(clock);
        repository.markSuccess(new MarkConsumeSuccessCommand(
                consumerGroup,
                event.eventId(),
                processedAt
        ));
    }

    private void markFailed(String consumerGroup,
                            EventEnvelope event,
                            ConsumeOptions options,
                            ConsumeRecord record,
                            Exception e) {
        Instant now = Instant.now(clock);
        int currentRetry = record != null ? record.retryCount() : 0;
        int nextRetry = currentRetry + 1;

        Duration backoff = computeBackoff(options, nextRetry);
        Instant nextRetryAt = now.plus(backoff);

        String rawMsg = e.getMessage();
        String msg = rawMsg == null ? e.toString() : rawMsg;
        repository.markFailed(new MarkConsumeFailedCommand(
                consumerGroup,
                event.eventId(),
                msg,
                nextRetryAt,
                nextRetry
        ));
    }

    private Duration computeBackoff(ConsumeOptions options, int retry) {
        Duration base = options.baseBackoff();
        Duration max = options.maxBackoff();
        if (base == null || base.isZero() || base.isNegative()) {
            base = Duration.ofSeconds(1);
        }
        if (max == null || max.isZero() || max.isNegative()) {
            max = Duration.ofMinutes(5);
        }
        // 简单指数退避：base * 2^(retry-1)
        long factor = 1L << Math.max(0, retry - 1);
        Duration candidate = base.multipliedBy(factor);
        if (candidate.compareTo(max) > 0) {
            candidate = max;
        }
        // 超过 maxRetry 时仍使用 maxBackoff，但不做额外处理，由上层根据 retryCount 决策
        return candidate;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

