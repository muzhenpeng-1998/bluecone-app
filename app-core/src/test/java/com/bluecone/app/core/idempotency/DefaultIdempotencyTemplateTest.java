package com.bluecone.app.core.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import com.bluecone.app.core.idempotency.api.IdempotencyConflictException;
import com.bluecone.app.core.idempotency.api.IdempotencyRequest;
import com.bluecone.app.core.idempotency.api.IdempotentResult;
import com.bluecone.app.core.idempotency.application.DefaultIdempotencyTemplate;
import com.bluecone.app.core.idempotency.domain.IdemStatus;
import com.bluecone.app.core.idempotency.domain.IdempotencyRecord;
import com.bluecone.app.core.idempotency.spi.IdempotencyLock;
import com.bluecone.app.core.idempotency.spi.IdempotencyMetrics;
import com.bluecone.app.core.idempotency.spi.IdempotencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * DefaultIdempotencyTemplate 行为单元测试，基于内存假实现模拟。
 */
class DefaultIdempotencyTemplateTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);

    private DefaultIdempotencyTemplate newTemplate(InMemoryIdempotencyRepository repo) {
        return new DefaultIdempotencyTemplate(repo, IdempotencyLock.noop(), IdempotencyMetrics.noop(),
                new ObjectMapper(), FIXED_CLOCK);
    }

    /**
     * 内存版幂等仓库实现，仅用于测试。
     */
    static class InMemoryIdempotencyRepository implements IdempotencyRepository {

        private final Map<String, IdempotencyRecord> store = new ConcurrentHashMap<>();
        private final Clock clock;

        InMemoryIdempotencyRepository(Clock clock) {
            this.clock = clock;
        }

        private String key(long tenantId, String bizType, String idemKey) {
            return tenantId + "|" + bizType + "|" + idemKey;
        }

        @Override
        public Optional<IdempotencyRecord> find(long tenantId, String bizType, String idemKey) {
            return Optional.ofNullable(store.get(key(tenantId, bizType, idemKey)));
        }

        @Override
        public AcquireResult tryAcquire(AcquireCommand command) {
            String k = key(command.tenantId(), command.bizType(), command.idemKey());
            Instant now = Instant.now(clock);
            IdempotencyRecord existing = store.get(k);
            if (existing == null) {
                IdempotencyRecord created = new IdempotencyRecord(
                        1L,
                        command.tenantId(),
                        command.bizType(),
                        command.idemKey(),
                        command.requestHash(),
                        IdemStatus.PROCESSING,
                        null,
                        null,
                        null,
                        null,
                        command.expireAt(),
                        command.lockUntil(),
                        0,
                        now,
                        now
                );
                store.put(k, created);
                return new AcquireResult(AcquireState.ACQUIRED, created);
            }

            // hash 不一致 => 冲突
            if (!existing.requestHash().equals(command.requestHash())) {
                return new AcquireResult(AcquireState.CONFLICT, existing);
            }

            if (existing.status() == IdemStatus.SUCCEEDED && existing.expireAt().isAfter(now)) {
                return new AcquireResult(AcquireState.REPLAY_SUCCEEDED, existing);
            }

            if (existing.status() == IdemStatus.PROCESSING && existing.lockUntil().isAfter(now)) {
                return new AcquireResult(AcquireState.IN_PROGRESS, existing);
            }

            // 简化：过期或租约过期 => 重新获得执行权
            IdempotencyRecord updated = new IdempotencyRecord(
                    existing.id(),
                    existing.tenantId(),
                    existing.bizType(),
                    existing.idemKey(),
                    existing.requestHash(),
                    IdemStatus.PROCESSING,
                    null,
                    null,
                    null,
                    null,
                    command.expireAt(),
                    command.lockUntil(),
                    existing.version() + 1,
                    existing.createdAt(),
                    now
            );
            store.put(k, updated);
            return new AcquireResult(AcquireState.ACQUIRED, updated);
        }

        @Override
        public void markSuccess(MarkSuccessCommand command) {
            String k = key(command.tenantId(), command.bizType(), command.idemKey());
            IdempotencyRecord existing = store.get(k);
            Instant now = Instant.now(clock);
            IdempotencyRecord updated = new IdempotencyRecord(
                    existing.id(),
                    existing.tenantId(),
                    existing.bizType(),
                    existing.idemKey(),
                    existing.requestHash(),
                    IdemStatus.SUCCEEDED,
                    command.resultRef(),
                    command.resultJson(),
                    null,
                    null,
                    command.expireAt(),
                    existing.lockUntil(),
                    existing.version() + 1,
                    existing.createdAt(),
                    now
            );
            store.put(k, updated);
        }

        @Override
        public void markFailed(MarkFailedCommand command) {
            String k = key(command.tenantId(), command.bizType(), command.idemKey());
            IdempotencyRecord existing = store.get(k);
            Instant now = Instant.now(clock);
            IdempotencyRecord updated = new IdempotencyRecord(
                    existing.id(),
                    existing.tenantId(),
                    existing.bizType(),
                    existing.idemKey(),
                    existing.requestHash(),
                    IdemStatus.FAILED,
                    existing.resultRef(),
                    existing.resultJson(),
                    command.errorCode(),
                    command.errorMsg(),
                    command.expireAt(),
                    existing.lockUntil(),
                    existing.version() + 1,
                    existing.createdAt(),
                    now
            );
            store.put(k, updated);
        }
    }

    @Test
    void sameKeySameHashShouldReplayResult() {
        InMemoryIdempotencyRepository repo = new InMemoryIdempotencyRepository(FIXED_CLOCK);
        DefaultIdempotencyTemplate template = newTemplate(repo);

        IdempotencyRequest req = new IdempotencyRequest(
                1L,
                "ORDER_CREATE",
                "key-1",
                "a".repeat(64),
                Duration.ofHours(24),
                Duration.ofSeconds(30),
                false,
                null
        );

        AtomicInteger counter = new AtomicInteger();

        IdempotentResult<String> r1 = template.execute(req, String.class, () -> {
            counter.incrementAndGet();
            return "OK";
        });
        assertFalse(r1.replayed());
        assertEquals("OK", r1.value());
        assertEquals(1, counter.get());

        IdempotentResult<String> r2 = template.execute(req, String.class, () -> {
            counter.incrementAndGet();
            return "OK-2";
        });
        assertTrue(r2.replayed());
        assertEquals("OK", r2.value());
        assertEquals(1, counter.get(), "supplier 不应在重放时再次执行");
    }

    @Test
    void sameKeyDifferentHashShouldConflict() {
        InMemoryIdempotencyRepository repo = new InMemoryIdempotencyRepository(FIXED_CLOCK);
        DefaultIdempotencyTemplate template = newTemplate(repo);

        IdempotencyRequest req1 = new IdempotencyRequest(
                1L, "ORDER_CREATE", "key-2", "a".repeat(64),
                Duration.ofHours(24), Duration.ofSeconds(30), false, null);
        IdempotencyRequest req2 = new IdempotencyRequest(
                1L, "ORDER_CREATE", "key-2", "b".repeat(64),
                Duration.ofHours(24), Duration.ofSeconds(30), false, null);

        template.execute(req1, String.class, () -> "OK");

        assertThrows(IdempotencyConflictException.class,
                () -> template.execute(req2, String.class, () -> "OK-2"));
    }

    @Test
    void concurrentRequestsOnlyExecuteOnce() throws InterruptedException {
        InMemoryIdempotencyRepository repo = new InMemoryIdempotencyRepository(FIXED_CLOCK);
        DefaultIdempotencyTemplate template = newTemplate(repo);

        IdempotencyRequest req = new IdempotencyRequest(
                1L, "ORDER_CREATE", "key-3", "c".repeat(64),
                Duration.ofHours(24), Duration.ofSeconds(30), false, null);

        AtomicInteger counter = new AtomicInteger();
        int threads = 2;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        Runnable task = () -> {
            try {
                start.await();
                template.execute(req, String.class, () -> {
                    counter.incrementAndGet();
                    return "OK";
                });
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        };

        new Thread(task).start();
        new Thread(task).start();
        start.countDown();
        done.await();

        assertEquals(1, counter.get(), "并发下 supplier 应只执行一次");
    }
}

