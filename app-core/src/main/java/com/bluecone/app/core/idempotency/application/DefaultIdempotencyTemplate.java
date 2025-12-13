package com.bluecone.app.core.idempotency.application;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

import com.bluecone.app.core.idempotency.api.IdempotentResult;
import com.bluecone.app.core.idempotency.api.IdempotencyConflictException;
import com.bluecone.app.core.idempotency.api.IdempotencyInProgressException;
import com.bluecone.app.core.idempotency.api.IdempotencyRequest;
import com.bluecone.app.core.idempotency.api.IdempotencyStorageException;
import com.bluecone.app.core.idempotency.api.IdempotencyTemplate;
import com.bluecone.app.core.idempotency.domain.IdemStatus;
import com.bluecone.app.core.idempotency.domain.IdempotencyRecord;
import com.bluecone.app.core.idempotency.spi.IdempotencyLock;
import com.bluecone.app.core.idempotency.spi.IdempotencyMetrics;
import com.bluecone.app.core.idempotency.spi.IdempotencyRepository;
import com.bluecone.app.core.idempotency.spi.IdempotencyRepository.AcquireCommand;
import com.bluecone.app.core.idempotency.spi.IdempotencyRepository.AcquireResult;
import com.bluecone.app.core.idempotency.spi.IdempotencyRepository.AcquireState;
import com.bluecone.app.core.idempotency.spi.IdempotencyRepository.MarkFailedCommand;
import com.bluecone.app.core.idempotency.spi.IdempotencyRepository.MarkSuccessCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 默认幂等执行模板实现。
 *
 * <p>基于数据库幂等记录 + 可选分布式锁实现高并发幂等控制，支持请求冲突检测与结果重放。</p>
 */
public class DefaultIdempotencyTemplate implements IdempotencyTemplate {

    private final IdempotencyRepository repository;
    private final IdempotencyLock lock;
    private final IdempotencyMetrics metrics;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public DefaultIdempotencyTemplate(IdempotencyRepository repository,
                                      IdempotencyLock lock,
                                      IdempotencyMetrics metrics,
                                      ObjectMapper objectMapper,
                                      Clock clock) {
        this.repository = repository;
        this.lock = (lock != null ? lock : IdempotencyLock.noop());
        this.metrics = (metrics != null ? metrics : IdempotencyMetrics.noop());
        this.objectMapper = objectMapper;
        this.clock = (clock != null ? clock : Clock.systemUTC());
    }

    @Override
    public <T> IdempotentResult<T> execute(IdempotencyRequest request, Class<T> resultType, Supplier<T> supplier) {
        Instant now = Instant.now(clock);
        Instant expireAt = now.plus(request.ttl());
        Instant lockUntil = now.plus(request.lockTtl());

        String lockKey = buildLockKey(request);
        boolean locked = false;
        try {
            locked = lock.tryLock(lockKey, request.lockTtl());
            if (!locked) {
                // 未获取到锁时，先尝试快速重放或返回处理中
                Optional<IdempotencyRecord> existingOpt =
                        repository.find(request.tenantId(), request.bizType(), request.idemKey());
                if (existingOpt.isPresent()) {
                    IdempotencyRecord existing = existingOpt.get();
                    // hash 冲突
                    if (!request.requestHash().equals(existing.requestHash())) {
                        metrics.recordConflict();
                        throw conflictException(request, existing);
                    }
                    if (existing.status() == IdemStatus.SUCCEEDED && existing.expireAt().isAfter(now)) {
                        metrics.recordReplay();
                        T value = deserializeResult(existing, resultType);
                        return new IdempotentResult<>(true, false, value);
                    }
                    if (existing.status() == IdemStatus.PROCESSING && existing.lockUntil().isAfter(now)) {
                        metrics.recordInProgress();
                        if (!request.waitForCompletion()) {
                            return new IdempotentResult<>(false, true, null);
                        }
                        return waitForCompletion(request, resultType, now);
                    }
                }
                // 走正常获取流程（可能是租约过期或记录不存在）
            }

            AcquireCommand command = new AcquireCommand(
                    request.tenantId(),
                    request.bizType(),
                    request.idemKey(),
                    request.requestHash(),
                    expireAt,
                    lockUntil
            );

            AcquireResult acquireResult = repository.tryAcquire(command);
            metrics.recordAcquire(acquireResult.state());

            return switch (acquireResult.state()) {
                case ACQUIRED -> executeBusinessAndPersist(request, resultType, supplier, expireAt);
                case REPLAY_SUCCEEDED -> {
                    metrics.recordReplay();
                    T value = deserializeResult(acquireResult.record(), resultType);
                    yield new IdempotentResult<>(true, false, value);
                }
                case CONFLICT -> {
                    metrics.recordConflict();
                    throw conflictException(request, acquireResult.record());
                }
                case IN_PROGRESS, RETRYABLE -> {
                    metrics.recordInProgress();
                    if (!request.waitForCompletion()) {
                        yield new IdempotentResult<>(false, true, null);
                    }
                    yield waitForCompletion(request, resultType, now);
                }
            };
        } catch (IdempotencyConflictException | IdempotencyInProgressException e) {
            throw e;
        } catch (Exception e) {
            throw new IdempotencyStorageException("幂等模板执行异常", e);
        } finally {
            if (locked) {
                lock.unlock(lockKey);
            }
        }
    }

    private String buildLockKey(IdempotencyRequest request) {
        return "idem:" + request.tenantId() + ":" + request.bizType() + ":" + request.idemKey();
    }

    private IdempotencyConflictException conflictException(IdempotencyRequest req, IdempotencyRecord record) {
        return new IdempotencyConflictException(
                "幂等冲突：tenantId=" + req.tenantId()
                        + ", bizType=" + req.bizType()
                        + ", idemKey=" + req.idemKey()
                        + " 的请求摘要与历史记录不一致");
    }

    private <T> IdempotentResult<T> executeBusinessAndPersist(IdempotencyRequest request,
                                                              Class<T> resultType,
                                                              Supplier<T> supplier,
                                                              Instant expireAt) {
        T value;
        try {
            value = supplier.get();
        } catch (RuntimeException ex) {
            // 业务异常：记录失败信息并抛出
            repository.markFailed(new MarkFailedCommand(
                    request.tenantId(),
                    request.bizType(),
                    request.idemKey(),
                    request.requestHash(),
                    null,
                    ex.getMessage(),
                    expireAt
            ));
            throw ex;
        } catch (Exception ex) {
            repository.markFailed(new MarkFailedCommand(
                    request.tenantId(),
                    request.bizType(),
                    request.idemKey(),
                    request.requestHash(),
                    null,
                    ex.getMessage(),
                    expireAt
            ));
            throw new IdempotencyStorageException("业务执行异常", ex);
        }

        ResultPayload payload = serializeResult(value);
        repository.markSuccess(new MarkSuccessCommand(
                request.tenantId(),
                request.bizType(),
                request.idemKey(),
                request.requestHash(),
                payload.ref(),
                payload.json(),
                expireAt
        ));
        return new IdempotentResult<>(false, false, value);
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
                // 尝试从对象上提取 publicId 之类的稳定引用
                String ref = extractRef(value);
                String truncated = json.substring(0, 4096);
                return new ResultPayload(ref, truncated);
            }
            return new ResultPayload(extractRef(value), json);
        } catch (JsonProcessingException e) {
            // JSON 序列化失败时，仅记录简要 toString 作为引用
            String ref = extractRef(value);
            return new ResultPayload(ref != null ? ref : value.toString(), null);
        }
    }

    private String extractRef(Object value) {
        try {
            // 优先尝试 publicId()/getPublicId() 方法
            var clazz = value.getClass();
            try {
                var m = clazz.getMethod("publicId");
                Object v = m.invoke(value);
                return v != null ? v.toString() : null;
            } catch (NoSuchMethodException ignored) {
            }
            try {
                var m = clazz.getMethod("getPublicId");
                Object v = m.invoke(value);
                return v != null ? v.toString() : null;
            } catch (NoSuchMethodException ignored) {
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private <T> IdempotentResult<T> waitForCompletion(IdempotencyRequest request,
                                                      Class<T> resultType,
                                                      Instant start) {
        Duration waitMax = request.waitMax();
        if (waitMax == null || waitMax.isZero() || waitMax.isNegative()) {
            // 未配置 waitMax，则直接返回处理中
            return new IdempotentResult<>(false, true, null);
        }
        Instant deadline = start.plus(waitMax);
        while (Instant.now(clock).isBefore(deadline)) {
            Optional<IdempotencyRecord> recordOpt =
                    repository.find(request.tenantId(), request.bizType(), request.idemKey());
            if (recordOpt.isEmpty()) {
                // 记录不存在，视为处理中
                sleepQuietly(50);
                continue;
            }
            IdempotencyRecord record = recordOpt.get();
            if (!request.requestHash().equals(record.requestHash())) {
                throw conflictException(request, record);
            }
            if (record.status() == IdemStatus.SUCCEEDED) {
                T value = deserializeResult(record, resultType);
                return new IdempotentResult<>(true, false, value);
            }
            if (record.status() == IdemStatus.FAILED) {
                throw new IdempotencyInProgressException(
                        "幂等请求之前执行失败，tenantId=" + request.tenantId()
                                + ", bizType=" + request.bizType()
                                + ", idemKey=" + request.idemKey());
            }
            sleepQuietly(50);
        }
        return new IdempotentResult<>(false, true, null);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private <T> T deserializeResult(IdempotencyRecord record, Class<T> resultType) {
        if (resultType == Void.class || resultType == Void.TYPE) {
            return null;
        }
        if (record.resultJson() != null) {
            try {
                return objectMapper.readValue(record.resultJson(), resultType);
            } catch (Exception e) {
                throw new IdempotencyStorageException("幂等结果 JSON 反序列化失败", e);
            }
        }
        if (record.resultRef() != null && resultType.isAssignableFrom(String.class)) {
            return resultType.cast(record.resultRef());
        }
        return null;
    }
}

