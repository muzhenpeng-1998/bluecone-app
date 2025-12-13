package com.bluecone.app.core.create.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import com.bluecone.app.core.create.api.CreateRequest;
import com.bluecone.app.core.create.api.CreateWork;
import com.bluecone.app.core.create.api.CreateWorkWithEvents;
import com.bluecone.app.core.create.api.CreateWorkWithEventsResult;
import com.bluecone.app.core.create.api.IdempotentCreateResult;
import com.bluecone.app.core.create.api.IdempotentCreateTemplate;
import com.bluecone.app.core.create.api.TxMode;
import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.idempotency.api.IdempotencyConflictException;
import com.bluecone.app.core.idempotency.api.IdempotencyInProgressException;
import com.bluecone.app.core.idempotency.api.IdempotencyStorageException;
import com.bluecone.app.core.idempotency.domain.IdemStatus;
import com.bluecone.app.core.idempotency.domain.IdempotencyRecord;
import com.bluecone.app.core.idempotency.spi.IdempotencyRepository;
import com.bluecone.app.core.idempotency.spi.IdempotencyRepository.AcquireCommand;
import com.bluecone.app.core.idempotency.spi.IdempotencyRepository.AcquireResult;
import com.bluecone.app.core.idempotency.spi.IdempotencyRepository.AcquireState;
import com.bluecone.app.core.idempotency.spi.IdempotencyRepository.MarkFailedCommand;
import com.bluecone.app.core.idempotency.spi.IdempotencyRepository.MarkSuccessCommand;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.publicid.api.DecodedPublicId;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 默认幂等创建模板实现。
 *
 * <p>统一生成 internal_id/public_id、执行业务并标记幂等成功，确保事务与幂等记录一致。</p>
 */
public class DefaultIdempotentCreateTemplate implements IdempotentCreateTemplate {

    private final IdService idService;
    private final PublicIdCodec publicIdCodec;
    private final IdempotencyRepository repository;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;
    private final Clock clock;
    private final DomainEventPublisher domainEventPublisher;

    public DefaultIdempotentCreateTemplate(IdService idService,
                                           PublicIdCodec publicIdCodec,
                                           IdempotencyRepository repository,
                                           PlatformTransactionManager transactionManager,
                                           ObjectMapper objectMapper,
                                           Clock clock,
                                           DomainEventPublisher domainEventPublisher) {
        this.idService = idService;
        this.publicIdCodec = publicIdCodec;
        this.repository = repository;
        this.transactionManager = transactionManager;
        this.objectMapper = objectMapper;
        this.clock = (clock != null ? clock : Clock.systemUTC());
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public <T> IdempotentCreateResult<T> create(CreateRequest request, CreateWork<T> work) {
        return doCreate(request, (internalId, publicId) -> {
            T value = work.execute(internalId, publicId);
            return new CreateWorkWithEventsResult<>(value, null);
        });
    }

    @Override
    public <T> IdempotentCreateResult<T> create(CreateRequest request, CreateWorkWithEvents<T> work) {
        return doCreate(request, work);
    }

    private <T> IdempotentCreateResult<T> doCreate(CreateRequest request,
                                                   CreateWorkWithEvents<T> work) {
        Instant now = Instant.now(clock);
        Instant expireAt = now.plus(request.ttl());
        Instant lockUntil = now.plus(request.lockTtl());

        AcquireCommand command = new AcquireCommand(
                request.tenantId(),
                request.bizType(),
                request.idemKey(),
                request.requestHash(),
                expireAt,
                lockUntil
        );

        AcquireResult acquireResult;
        try {
            acquireResult = repository.tryAcquire(command);
        } catch (Exception e) {
            throw new IdempotencyStorageException("幂等获取执行权失败", e);
        }

        return switch (acquireResult.state()) {
            case CONFLICT -> throw conflictException(request, acquireResult.record());
            case REPLAY_SUCCEEDED -> replayExisting(request, acquireResult.record(), request.resourceType());
            case IN_PROGRESS, RETRYABLE -> handleInProgress(request);
            case ACQUIRED -> executeNewCreate(request, work, expireAt);
        };
    }

    private IdempotencyConflictException conflictException(CreateRequest req, IdempotencyRecord record) {
        return new IdempotencyConflictException(
                "幂等冲突：tenantId=" + req.tenantId()
                        + ", bizType=" + req.bizType()
                        + ", idemKey=" + req.idemKey()
                        + " 的请求摘要与历史记录不一致");
    }

    private <T> IdempotentCreateResult<T> replayExisting(CreateRequest req,
                                                         IdempotencyRecord record,
                                                         String expectedResourceType) {
        String publicId = record.resultRef();
        Ulid128 internalId = null;
        if (publicId != null) {
            DecodedPublicId decoded = publicIdCodec.decode(publicId);
            if (!expectedResourceType.equals(decoded.type())) {
                throw new IdempotencyConflictException(
                        "public_id 类型不匹配，期望=" + expectedResourceType + "，实际=" + decoded.type());
            }
            internalId = decoded.id();
        }

        T value = null;
        if (record.resultJson() != null) {
            try {
                @SuppressWarnings("unchecked")
                T parsed = (T) objectMapper.readTree(record.resultJson());
                value = parsed;
            } catch (Exception e) {
                // 结果 JSON 仅为辅助信息，解析失败可以忽略或抛出；此处保守抛出存储异常
                throw new IdempotencyStorageException("幂等记录结果反序列化失败", e);
            }
        }
        return new IdempotentCreateResult<>(true, false, publicId, internalId, value);
    }

    private <T> IdempotentCreateResult<T> handleInProgress(CreateRequest request) {
        if (!request.waitForCompletion()) {
            return new IdempotentCreateResult<>(false, true, null, null, null);
        }
        // waitForCompletion=true => 轮询等待
        Duration waitMax = request.waitMax();
        if (waitMax == null || waitMax.isZero() || waitMax.isNegative()) {
            return new IdempotentCreateResult<>(false, true, null, null, null);
        }
        Instant start = Instant.now(clock);
        Instant deadline = start.plus(waitMax);
        while (Instant.now(clock).isBefore(deadline)) {
            Optional<IdempotencyRecord> recordOpt =
                    repository.find(request.tenantId(), request.bizType(), request.idemKey());
            if (recordOpt.isEmpty()) {
                sleepQuietly(50);
                continue;
            }
            IdempotencyRecord record = recordOpt.get();
            if (!request.requestHash().equals(record.requestHash())) {
                throw conflictException(request, record);
            }
            if (record.status() == IdemStatus.SUCCEEDED) {
                return replayExisting(request, record, request.resourceType());
            }
            if (record.status() == IdemStatus.FAILED) {
                throw new IdempotencyInProgressException(
                        "幂等请求之前执行失败，tenantId=" + request.tenantId()
                                + ", bizType=" + request.bizType()
                                + ", idemKey=" + request.idemKey());
            }
            sleepQuietly(50);
        }
        return new IdempotentCreateResult<>(false, true, null, null, null);
    }

    private <T> IdempotentCreateResult<T> executeNewCreate(CreateRequest request,
                                                           CreateWorkWithEvents<T> work,
                                                           Instant expireAt) {
        // 生成 internal_id 与 public_id
        Ulid128 internalId = idService.nextUlid();
        String publicId = publicIdCodec.encode(request.resourceType(), internalId).asString();

        TxMode mode = request.txMode();
        try {
            CreateWorkWithEventsResult<T> result;
            switch (mode) {
                case REQUIRES_NEW -> result = executeInTx(TransactionDefinition.PROPAGATION_REQUIRES_NEW,
                        internalId, publicId, request, work, expireAt);
                case REQUIRED -> result = executeInTx(TransactionDefinition.PROPAGATION_REQUIRED,
                        internalId, publicId, request, work, expireAt);
                case AWARE_ONLY -> result = executeWithoutTx(internalId, publicId, request, work, expireAt);
                default -> throw new IllegalArgumentException("未知 TxMode: " + mode);
            }
            return new IdempotentCreateResult<>(false, false, publicId, internalId, result.value());
        } catch (RuntimeException e) {
            markFailedInNewTx(request, e.getMessage(), expireAt);
            throw e;
        } catch (Exception e) {
            markFailedInNewTx(request, e.getMessage(), expireAt);
            throw new IdempotencyStorageException("幂等创建业务执行异常", e);
        }
    }

    private <T> CreateWorkWithEventsResult<T> executeInTx(int propagationBehavior,
                                                          Ulid128 internalId,
                                                          String publicId,
                                                          CreateRequest request,
                                                          CreateWorkWithEvents<T> work,
                                                          Instant expireAt) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(propagationBehavior);
        return template.execute(status -> {
            CreateWorkWithEventsResult<T> result = work.execute(internalId, publicId);
            ResultPayload payload = serializeResult(result.value());
            repository.markSuccess(new MarkSuccessCommand(
                    request.tenantId(),
                    request.bizType(),
                    request.idemKey(),
                    request.requestHash(),
                    publicId,               // result_ref 必须是 publicId
                    payload.json(),
                    expireAt
            ));
            publishEvents(result.events());
            return result;
        });
    }

    private <T> CreateWorkWithEventsResult<T> executeWithoutTx(Ulid128 internalId,
                                                               String publicId,
                                                               CreateRequest request,
                                                               CreateWorkWithEvents<T> work,
                                                               Instant expireAt) {
        CreateWorkWithEventsResult<T> result = work.execute(internalId, publicId);
        ResultPayload payload = serializeResult(result.value());
        repository.markSuccess(new MarkSuccessCommand(
                request.tenantId(),
                request.bizType(),
                request.idemKey(),
                request.requestHash(),
                publicId,
                payload.json(),
                expireAt
        ));
        publishEvents(result.events());
        return result;
    }

    private record ResultPayload(String ref, String json) {
    }

    private <T> ResultPayload serializeResult(T value) {
        if (value == null) {
            return new ResultPayload(null, null);
        }
        if (value instanceof String s) {
            return new ResultPayload(s, null);
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            if (json != null && json.length() > 4096) {
                String truncated = json.substring(0, 4096);
                return new ResultPayload(null, truncated);
            }
            return new ResultPayload(null, json);
        } catch (Exception e) {
            return new ResultPayload(value.toString(), null);
        }
    }

    private void markFailedInNewTx(CreateRequest request, String errorMsg, Instant expireAt) {
        if (transactionManager == null) {
            repository.markFailed(new MarkFailedCommand(
                    request.tenantId(),
                    request.bizType(),
                    request.idemKey(),
                    request.requestHash(),
                    null,
                    errorMsg,
                    expireAt
            ));
            return;
        }
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.execute(status -> {
            repository.markFailed(new MarkFailedCommand(
                    request.tenantId(),
                    request.bizType(),
                    request.idemKey(),
                    request.requestHash(),
                    null,
                    errorMsg,
                    expireAt
            ));
            return null;
        });
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void publishEvents(java.util.List<DomainEvent> events) {
        if (events == null || events.isEmpty() || domainEventPublisher == null) {
            return;
        }
        for (DomainEvent event : events) {
            domainEventPublisher.publish(event);
        }
    }
}
