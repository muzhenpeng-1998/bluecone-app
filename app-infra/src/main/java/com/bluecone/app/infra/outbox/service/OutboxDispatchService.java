// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/service/OutboxDispatchService.java
package com.bluecone.app.infra.outbox.service;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventHandler;
import com.bluecone.app.infra.outbox.config.OutboxProperties;
import com.bluecone.app.infra.outbox.core.EventConsumptionTracker;
import com.bluecone.app.infra.outbox.core.EventSerializer;
import com.bluecone.app.infra.outbox.core.OutboxMetricsRecorder;
import com.bluecone.app.infra.outbox.core.RetryPolicy;
import com.bluecone.app.infra.outbox.entity.OutboxMessageEntity;
import com.bluecone.app.infra.outbox.entity.OutboxMessageStatus;
import com.bluecone.app.infra.outbox.repository.OutboxMessageRepository;
import com.bluecone.app.infra.outbox.router.OutboxEventRouter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 负责扫描 Outbox 表并异步（应用内）投递事件，包含状态流转与重试。
 */
@Service
public class OutboxDispatchService {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatchService.class);

    private final OutboxMessageRepository repository;
    private final EventSerializer serializer;
    private final OutboxEventRouter router;
    private final RetryPolicy retryPolicy;
    private final ObjectMapper objectMapper;
    private final OutboxProperties properties;
    private final EventConsumptionTracker consumptionTracker;
    private final OutboxMetricsRecorder metricsRecorder;

    public OutboxDispatchService(final OutboxMessageRepository repository,
                                 final EventSerializer serializer,
                                 final OutboxEventRouter router,
                                 final RetryPolicy retryPolicy,
                                 final ObjectMapper objectMapper,
                                 final OutboxProperties properties,
                                 final EventConsumptionTracker consumptionTracker,
                                 final OutboxMetricsRecorder metricsRecorder) {
        this.repository = repository;
        this.serializer = serializer;
        this.router = router;
        this.retryPolicy = retryPolicy;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.consumptionTracker = consumptionTracker;
        this.metricsRecorder = metricsRecorder;
    }

    public void dispatchDueMessages() {
        List<OutboxMessageEntity> due = repository.findDueMessages(
                List.of(OutboxMessageStatus.NEW, OutboxMessageStatus.FAILED),
                LocalDateTime.now(),
                properties.getDispatchBatchSize());
        for (OutboxMessageEntity message : due) {
            dispatchSingle(message);
        }
    }

    private void dispatchSingle(final OutboxMessageEntity message) {
        Objects.requireNonNull(message, "message must not be null");
        if (!repository.markPublished(message.getId())) {
            log.warn("[Outbox] mark published failed id={}", message.getId());
        }
        Map<String, String> headers = deserializeHeaders(message);
        DomainEvent event = serializer.deserialize(message.getPayload(), headers);

        String traceId = headers.get("traceId");
        if (traceId != null) {
            MDC.put("traceId", traceId);
        }
        try {
            List<EventHandler<?>> handlers = router.route(event);
            for (EventHandler<?> handler : handlers) {
                if (consumptionTracker != null) {
                    boolean first = consumptionTracker.tryMarkProcessing(handler.getClass().getSimpleName(), event.getEventId());
                    if (!first) {
                        continue;
                    }
                }
                invokeHandler(handler, event);
            }
            repository.markDone(message.getId());
            message.setStatus(OutboxMessageStatus.DONE);
            metricsRecorder.onPublishedSuccess(message);
            log.info("[Outbox] delivered eventType={} eventKey={} outboxId={}", message.getEventType(), message.getEventKey(), message.getId());
        } catch (Exception ex) {
            handleFailure(message, ex);
        } finally {
            MDC.clear();
        }
    }

    private Map<String, String> deserializeHeaders(final OutboxMessageEntity message) {
        try {
            return objectMapper.readValue(message.getHeaders(), new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize outbox headers for id=" + message.getId(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <E extends DomainEvent> void invokeHandler(final EventHandler<?> handler, final DomainEvent event) {
        EventHandler<E> typed = (EventHandler<E>) handler;
        typed.handle((E) event);
    }

    private void handleFailure(final OutboxMessageEntity message, final Exception ex) {
        int nextRetry = (message.getRetryCount() == null ? 0 : message.getRetryCount()) + 1;
        boolean dead = retryPolicy.shouldGiveUp(nextRetry, ex);
        LocalDateTime nextRetryAt = dead ? null : LocalDateTime.now().plus(retryPolicy.nextDelay(nextRetry));
        repository.markFailed(message.getId(), nextRetry, nextRetryAt, dead);
        message.setRetryCount(nextRetry);
        message.setStatus(dead ? OutboxMessageStatus.DEAD : OutboxMessageStatus.FAILED);
        if (dead) {
            metricsRecorder.onDeadLetter(message, ex);
        } else {
            metricsRecorder.onPublishedFailure(message, ex);
        }
        log.error("[Outbox] dispatch failed outboxId={} eventType={} retry={} dead={}",
                message.getId(), message.getEventType(), nextRetry, dead, ex);
    }
}
