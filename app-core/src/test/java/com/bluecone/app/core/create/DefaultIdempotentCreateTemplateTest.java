package com.bluecone.app.core.create;

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
import java.util.concurrent.atomic.AtomicInteger;

import com.bluecone.app.core.create.api.CreateRequest;
import com.bluecone.app.core.create.api.CreateWork;
import com.bluecone.app.core.create.api.CreateWorkWithEvents;
import com.bluecone.app.core.create.api.CreateWorkWithEventsResult;
import com.bluecone.app.core.create.api.IdempotentCreateResult;
import com.bluecone.app.core.create.api.TxMode;
import com.bluecone.app.core.create.application.DefaultIdempotentCreateTemplate;
import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.idempotency.api.IdempotencyConflictException;
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
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * DefaultIdempotentCreateTemplate 单元测试。
 */
class DefaultIdempotentCreateTemplateTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);

    private PlatformTransactionManager newTxManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
                // no-op
            }

            @Override
            public void rollback(TransactionStatus status) {
                // no-op
            }
        };
    }

    private DefaultIdempotentCreateTemplate newTemplate(InMemoryRepo repo,
                                                        IdService idService,
                                                        PublicIdCodec codec,
                                                        DomainEventPublisher publisher) {
        PlatformTransactionManager txManager = newTxManager();
        return new DefaultIdempotentCreateTemplate(
                idService,
                codec,
                repo,
                txManager,
                new ObjectMapper(),
                FIXED_CLOCK,
                publisher
        );
    }

    /**
     * 内存版 IdempotencyRepository，仅用于测试。
     */
    static class InMemoryRepo implements IdempotencyRepository {

        private final Map<String, IdempotencyRecord> store = new ConcurrentHashMap<>();
        private final Clock clock;
        volatile boolean markSuccessCalled = false;
        volatile boolean markFailedCalled = false;

        InMemoryRepo(Clock clock) {
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
            if (!existing.requestHash().equals(command.requestHash())) {
                return new AcquireResult(AcquireState.CONFLICT, existing);
            }
            if (existing.status() == IdemStatus.SUCCEEDED && existing.expireAt().isAfter(now)) {
                return new AcquireResult(AcquireState.REPLAY_SUCCEEDED, existing);
            }
            if (existing.status() == IdemStatus.PROCESSING && existing.lockUntil().isAfter(now)) {
                return new AcquireResult(AcquireState.IN_PROGRESS, existing);
            }
            // 简化：其余情况视为 ACQUIRED
            return new AcquireResult(AcquireState.ACQUIRED, existing);
        }

        @Override
        public void markSuccess(MarkSuccessCommand command) {
            String k = key(command.tenantId(), command.bizType(), command.idemKey());
            Instant now = Instant.now(clock);
            IdempotencyRecord existing = store.get(k);
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
            markSuccessCalled = true;
        }

        @Override
        public void markFailed(MarkFailedCommand command) {
            String k = key(command.tenantId(), command.bizType(), command.idemKey());
            Instant now = Instant.now(clock);
            IdempotencyRecord existing = store.get(k);
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
            markFailedCalled = true;
        }
    }

    private IdService stubIdService(AtomicInteger counter) {
        return new IdService() {
            @Override
            public Ulid128 nextUlid() {
                int n = counter.incrementAndGet();
                return new Ulid128(n, n);
            }

            @Override
            public String nextUlidString() {
                return "STUB-" + counter.get();
            }

            @Override
            public byte[] nextUlidBytes() {
                return new byte[16];
            }
        };
    }

    private PublicIdCodec stubPublicIdCodec() {
        return new PublicIdCodec() {
            @Override
            public com.bluecone.app.id.publicid.api.PublicId encode(String type, Ulid128 id) {
                String v = type + "_" + id.msb() + "_" + id.lsb();
                return new com.bluecone.app.id.publicid.api.PublicId(type, v);
            }

            @Override
            public com.bluecone.app.id.publicid.api.PublicId encode(String type, byte[] ulidBytes16) {
                return new com.bluecone.app.id.publicid.api.PublicId(type, type + "_bytes");
            }

            @Override
            public DecodedPublicId decode(String publicId) {
                String[] parts = publicId.split("_");
                String type = parts[0];
                long msb = Long.parseLong(parts[1]);
                long lsb = Long.parseLong(parts[2]);
                return new DecodedPublicId(type, new Ulid128(msb, lsb));
            }

            @Override
            public boolean isValid(String publicId) {
                return true;
            }
        };
    }

    @Test
    void firstCreateShouldSucceedAndMarkSuccess() {
        InMemoryRepo repo = new InMemoryRepo(FIXED_CLOCK);
        AtomicInteger idCounter = new AtomicInteger();
        IdService idService = stubIdService(idCounter);
        PublicIdCodec codec = stubPublicIdCodec();
        DefaultIdempotentCreateTemplate template = newTemplate(repo, idService, codec, event -> {});

        CreateRequest req = new CreateRequest(
                1L,
                "ORDER_CREATE",
                "ord",
                "key-1",
                "a".repeat(64),
                Duration.ofHours(24),
                Duration.ofSeconds(30),
                TxMode.REQUIRES_NEW,
                false,
                null
        );

        IdempotentCreateResult<String> result = template.create(
                req,
                (CreateWork<String>) (internalId, publicId) -> "OK"
        );

        assertFalse(result.replayed());
        assertFalse(result.inProgress());
        assertEquals("OK", result.value());
        assertEquals(1, idCounter.get());
        assertTrue(repo.markSuccessCalled);
        assertEquals("ord_1_1", result.publicId());
    }

    @Test
    void sameKeyDifferentHashShouldConflict() {
        InMemoryRepo repo = new InMemoryRepo(FIXED_CLOCK);
        AtomicInteger idCounter = new AtomicInteger();
        IdService idService = stubIdService(idCounter);
        PublicIdCodec codec = stubPublicIdCodec();
        DefaultIdempotentCreateTemplate template = newTemplate(repo, idService, codec, event -> {});

        CreateRequest req1 = new CreateRequest(
                1L, "ORDER_CREATE", "ord", "key-2", "a".repeat(64),
                Duration.ofHours(24), Duration.ofSeconds(30), TxMode.REQUIRES_NEW, false, null);
        CreateRequest req2 = new CreateRequest(
                1L, "ORDER_CREATE", "ord", "key-2", "b".repeat(64),
                Duration.ofHours(24), Duration.ofSeconds(30), TxMode.REQUIRES_NEW, false, null);

        template.create(req1, (CreateWork<String>) (id, pub) -> "OK");

        assertThrows(IdempotencyConflictException.class,
                () -> template.create(req2, (CreateWork<String>) (id, pub) -> "OK-2"));
    }

    @Test
    void conflictShouldNotCallWorkOrMarkSuccess() {
        InMemoryRepo repo = new InMemoryRepo(FIXED_CLOCK) {
            @Override
            public AcquireResult tryAcquire(AcquireCommand command) {
                // 构造一个已有记录且 hash 不同的情形，直接返回 CONFLICT
                IdempotencyRecord record = new IdempotencyRecord(
                        1L, command.tenantId(), command.bizType(), command.idemKey(),
                        "x".repeat(64), IdemStatus.SUCCEEDED,
                        null, null, null, null,
                        command.expireAt(), command.lockUntil(), 0,
                        Instant.now(FIXED_CLOCK), Instant.now(FIXED_CLOCK));
                return new AcquireResult(AcquireState.CONFLICT, record);
            }
        };
        AtomicInteger idCounter = new AtomicInteger();
        IdService idService = stubIdService(idCounter);
        PublicIdCodec codec = stubPublicIdCodec();
        DefaultIdempotentCreateTemplate template = newTemplate(repo, idService, codec, event -> {});

        CreateRequest req = new CreateRequest(
                1L, "ORDER_CREATE", "ord", "key-3", "y".repeat(64),
                Duration.ofHours(24), Duration.ofSeconds(30), TxMode.REQUIRES_NEW, false, null);

        assertThrows(IdempotencyConflictException.class,
                () -> template.create(req, (CreateWork<String>) (id, pub) -> "OK"));
        assertEquals(0, idCounter.get(), "冲突情况下不应生成 ID");
        assertFalse(repo.markSuccessCalled, "冲突情况下不应 markSuccess");
    }

    @Test
    void createWithEventsShouldPublishAfterSuccess() {
        InMemoryRepo repo = new InMemoryRepo(FIXED_CLOCK);
        AtomicInteger idCounter = new AtomicInteger();
        IdService idService = stubIdService(idCounter);
        PublicIdCodec codec = stubPublicIdCodec();
        AtomicInteger published = new AtomicInteger();
        DomainEventPublisher publisher = new DomainEventPublisher() {
            @Override
            public void publish(DomainEvent event) {
                published.incrementAndGet();
            }
        };
        DefaultIdempotentCreateTemplate template = newTemplate(repo, idService, codec, publisher);

        CreateRequest req = new CreateRequest(
                1L,
                "ORDER_CREATE",
                "ord",
                "key-ev",
                "a".repeat(64),
                Duration.ofHours(24),
                Duration.ofSeconds(30),
                TxMode.REQUIRES_NEW,
                false,
                null
        );

        IdempotentCreateResult<String> result = template.create(
                req,
                (CreateWorkWithEvents<String>) (internalId, publicId) -> {
                    DomainEvent event = new DomainEvent("order.created", null) {
                    };
                    return new CreateWorkWithEventsResult<>("OK", java.util.List.of(event));
                }
        );

        assertFalse(result.replayed());
        assertFalse(result.inProgress());
        assertEquals("OK", result.value());
        assertEquals(1, published.get(), "应发布一个领域事件");
    }
}
