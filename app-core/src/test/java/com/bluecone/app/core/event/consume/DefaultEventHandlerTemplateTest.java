package com.bluecone.app.core.event.consume;

import com.bluecone.app.core.event.consume.api.ConsumeOptions;
import com.bluecone.app.core.event.consume.api.ConsumeResult;
import com.bluecone.app.core.event.consume.api.EventConsumeConflictException;
import com.bluecone.app.core.event.consume.api.EventConsumeFailedException;
import com.bluecone.app.core.event.consume.api.EventEnvelope;
import com.bluecone.app.core.event.consume.api.EventHandler;
import com.bluecone.app.core.event.consume.application.DefaultEventHandlerTemplate;
import com.bluecone.app.core.event.consume.spi.ConsumeMetrics;
import com.bluecone.app.core.event.consume.spi.EventDedupRepository;
import com.bluecone.app.id.core.Ulid128;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultEventHandlerTemplate 单元测试。
 */
class DefaultEventHandlerTemplateTest {

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

    private DefaultEventHandlerTemplate newTemplate(EventDedupRepository repo) {
        PlatformTransactionManager txManager = newTxManager();
        return new DefaultEventHandlerTemplate(repo, txManager, FIXED_CLOCK, ConsumeMetrics.noop());
    }

    /**
     * 内存版 EventDedupRepository，仅用于测试。
     */
    static class InMemoryRepo implements EventDedupRepository {

        private final Map<String, ConsumeRecord> store = new ConcurrentHashMap<>();
        private final Clock clock;

        InMemoryRepo(Clock clock) {
            this.clock = clock;
        }

        private String key(String group, Ulid128 eventId) {
            return group + "|" + eventId.msb() + "|" + eventId.lsb();
        }

        @Override
        public AcquireConsumeResult tryAcquire(AcquireConsumeCommand command) {
            String k = key(command.consumerGroup(), command.eventId());
            ConsumeRecord existing = store.get(k);
            Instant now = Instant.now(clock);
            if (existing == null) {
                ConsumeRecord created = new ConsumeRecord(
                        command.tenantId(),
                        command.consumerGroup(),
                        command.eventId(),
                        command.eventType(),
                        0,
                        command.lockedUntil(),
                        now,
                        0,
                        null
                );
                store.put(k, created);
                return new AcquireConsumeResult(AcquireConsumeState.ACQUIRED, created);
            }
            if (!existing.eventType().equals(command.eventType())) {
                return new AcquireConsumeResult(AcquireConsumeState.CONFLICT, existing);
            }
            if (existing.status() == 1) {
                return new AcquireConsumeResult(AcquireConsumeState.REPLAY_SUCCEEDED, existing);
            }
            if (existing.status() == 0 && existing.lockedUntil().isAfter(now)) {
                return new AcquireConsumeResult(AcquireConsumeState.IN_PROGRESS, existing);
            }
            if (existing.status() == 2 && existing.nextRetryAt().isAfter(now)) {
                return new AcquireConsumeResult(AcquireConsumeState.IN_PROGRESS, existing);
            }
            ConsumeRecord updated = new ConsumeRecord(
                    existing.tenantId(),
                    existing.consumerGroup(),
                    existing.eventId(),
                    existing.eventType(),
                    0,
                    command.lockedUntil(),
                    existing.nextRetryAt(),
                    existing.retryCount(),
                    existing.errorMsg()
            );
            store.put(k, updated);
            return new AcquireConsumeResult(
                    existing.status() == 2 ? AcquireConsumeState.RETRYABLE_FAILED : AcquireConsumeState.ACQUIRED,
                    updated
            );
        }

        @Override
        public Optional<ConsumeRecord> find(String consumerGroup, Ulid128 eventId) {
            return Optional.ofNullable(store.get(key(consumerGroup, eventId)));
        }

        @Override
        public void markSuccess(MarkConsumeSuccessCommand command) {
            String k = key(command.consumerGroup(), command.eventId());
            ConsumeRecord existing = store.get(k);
            ConsumeRecord updated = new ConsumeRecord(
                    existing.tenantId(),
                    existing.consumerGroup(),
                    existing.eventId(),
                    existing.eventType(),
                    1,
                    existing.lockedUntil(),
                    existing.nextRetryAt(),
                    existing.retryCount(),
                    existing.errorMsg()
            );
            store.put(k, updated);
        }

        @Override
        public void markFailed(MarkConsumeFailedCommand command) {
            String k = key(command.consumerGroup(), command.eventId());
            ConsumeRecord existing = store.get(k);
            ConsumeRecord updated = new ConsumeRecord(
                    existing.tenantId(),
                    existing.consumerGroup(),
                    existing.eventId(),
                    existing.eventType(),
                    2,
                    existing.lockedUntil(),
                    command.nextRetryAt(),
                    command.retryCount(),
                    command.errorMsg()
            );
            store.put(k, updated);
        }
    }

    private ConsumeOptions defaultOptions() {
        return new ConsumeOptions(
                Duration.ofSeconds(30),
                false,
                Duration.ZERO,
                20,
                Duration.ofSeconds(1),
                Duration.ofMinutes(5)
        );
    }

    @Test
    void firstConsumeShouldSucceedAndMarkSuccess() {
        InMemoryRepo repo = new InMemoryRepo(FIXED_CLOCK);
        DefaultEventHandlerTemplate template = newTemplate(repo);
        AtomicBoolean handled = new AtomicBoolean(false);

        EventEnvelope envelope = new EventEnvelope(
                1L,
                new Ulid128(1L, 2L),
                "ORDER_CREATED",
                "{}",
                "{}",
                Instant.now(FIXED_CLOCK)
        );

        ConsumeResult result = template.consume(
                "ORDER",
                envelope,
                evt -> handled.set(true),
                defaultOptions()
        );

        assertTrue(handled.get());
        assertFalse(result.replayed());
        assertFalse(result.inProgress());
        assertTrue(result.succeeded());
        assertEquals(0, result.retryCount());
        assertEquals(1, repo.find("ORDER", envelope.eventId()).orElseThrow().status());
    }

    @Test
    void replayShouldNotInvokeHandler() {
        InMemoryRepo repo = new InMemoryRepo(FIXED_CLOCK);
        DefaultEventHandlerTemplate template = newTemplate(repo);
        AtomicInteger count = new AtomicInteger();

        EventEnvelope envelope = new EventEnvelope(
                1L,
                new Ulid128(10L, 20L),
                "ORDER_CREATED",
                "{}",
                "{}",
                Instant.now(FIXED_CLOCK)
        );

        template.consume("ORDER", envelope, evt -> count.incrementAndGet(), defaultOptions());

        ConsumeResult result = template.consume(
                "ORDER",
                envelope,
                evt -> count.incrementAndGet(),
                defaultOptions()
        );

        assertEquals(1, count.get());
        assertTrue(result.replayed());
        assertFalse(result.inProgress());
        assertTrue(result.succeeded());
    }

    @Test
    void conflictShouldThrowException() {
        InMemoryRepo repo = new InMemoryRepo(FIXED_CLOCK) {
            @Override
            public AcquireConsumeResult tryAcquire(AcquireConsumeCommand command) {
                ConsumeRecord record = new ConsumeRecord(
                        command.tenantId(),
                        command.consumerGroup(),
                        command.eventId(),
                        "OTHER_TYPE",
                        0,
                        command.lockedUntil(),
                        command.now(),
                        0,
                        null
                );
                return new AcquireConsumeResult(AcquireConsumeState.CONFLICT, record);
            }
        };
        DefaultEventHandlerTemplate template = newTemplate(repo);
        EventEnvelope envelope = new EventEnvelope(
                1L,
                new Ulid128(1L, 1L),
                "ORDER_CREATED",
                "{}",
                "{}",
                Instant.now(FIXED_CLOCK)
        );

        assertThrows(EventConsumeConflictException.class, () ->
                template.consume("ORDER", envelope, evt -> {}, defaultOptions()));
    }

    @Test
    void inProgressWithoutWaitShouldReturnInProgress() {
        InMemoryRepo repo = new InMemoryRepo(FIXED_CLOCK) {
            @Override
            public AcquireConsumeResult tryAcquire(AcquireConsumeCommand command) {
                ConsumeRecord record = new ConsumeRecord(
                        command.tenantId(),
                        command.consumerGroup(),
                        command.eventId(),
                        command.eventType(),
                        0,
                        command.lockedUntil().plusSeconds(60),
                        command.now(),
                        0,
                        null
                );
                return new AcquireConsumeResult(AcquireConsumeState.IN_PROGRESS, record);
            }
        };
        DefaultEventHandlerTemplate template = newTemplate(repo);
        EventEnvelope envelope = new EventEnvelope(
                1L,
                new Ulid128(2L, 3L),
                "ORDER_CREATED",
                "{}",
                "{}",
                Instant.now(FIXED_CLOCK)
        );

        ConsumeResult result = template.consume("ORDER", envelope, evt -> {
            fail("handler should not be invoked when IN_PROGRESS without wait");
        }, defaultOptions());

        assertFalse(result.replayed());
        assertTrue(result.inProgress());
        assertFalse(result.succeeded());
    }

    @Test
    void waitModeShouldReplayAfterSuccess() {
        AtomicInteger findCount = new AtomicInteger();
        InMemoryRepo repo = new InMemoryRepo(FIXED_CLOCK) {
            @Override
            public AcquireConsumeResult tryAcquire(AcquireConsumeCommand command) {
                ConsumeRecord record = new ConsumeRecord(
                        command.tenantId(),
                        command.consumerGroup(),
                        command.eventId(),
                        command.eventType(),
                        0,
                        command.lockedUntil().plusSeconds(60),
                        command.now(),
                        0,
                        null
                );
                return new AcquireConsumeResult(AcquireConsumeState.IN_PROGRESS, record);
            }

            @Override
            public Optional<ConsumeRecord> find(String consumerGroup, Ulid128 eventId) {
                int c = findCount.incrementAndGet();
                if (c == 1) {
                    return Optional.of(new ConsumeRecord(
                            1L, consumerGroup, eventId, "ORDER_CREATED",
                            0, Instant.now(FIXED_CLOCK).plusSeconds(60),
                            Instant.now(FIXED_CLOCK),
                            0, null
                    ));
                }
                return Optional.of(new ConsumeRecord(
                        1L, consumerGroup, eventId, "ORDER_CREATED",
                        1, Instant.now(FIXED_CLOCK),
                        Instant.now(FIXED_CLOCK),
                        0, null
                ));
            }
        };
        DefaultEventHandlerTemplate template = newTemplate(repo);
        EventEnvelope envelope = new EventEnvelope(
                1L,
                new Ulid128(5L, 6L),
                "ORDER_CREATED",
                "{}",
                "{}",
                Instant.now(FIXED_CLOCK)
        );

        ConsumeOptions options = new ConsumeOptions(
                Duration.ofSeconds(30),
                true,
                Duration.ofMillis(500),
                20,
                Duration.ofSeconds(1),
                Duration.ofMinutes(5)
        );

        ConsumeResult result = template.consume("ORDER", envelope, evt -> {
            fail("handler should not be invoked in wait replay mode");
        }, options);

        assertTrue(result.replayed());
        assertFalse(result.inProgress());
        assertTrue(result.succeeded());
    }

    @Test
    void handlerFailureShouldMarkFailedAndPropagate() {
        InMemoryRepo repo = new InMemoryRepo(FIXED_CLOCK);
        DefaultEventHandlerTemplate template = newTemplate(repo);
        EventEnvelope envelope = new EventEnvelope(
                1L,
                new Ulid128(7L, 8L),
                "ORDER_CREATED",
                "{}",
                "{}",
                Instant.now(FIXED_CLOCK)
        );

        ConsumeOptions options = defaultOptions();

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                template.consume("ORDER", envelope, evt -> {
                    throw new RuntimeException("boom");
                }, options));

        assertEquals("boom", ex.getMessage());
        EventDedupRepository.ConsumeRecord record =
                repo.find("ORDER", envelope.eventId()).orElseThrow();
        assertEquals(2, record.status());
        assertEquals(1, record.retryCount());
        assertNotNull(record.nextRetryAt());
    }

    @Test
    void waitModeShouldThrowWhenFailed() {
        InMemoryRepo repo = new InMemoryRepo(FIXED_CLOCK) {
            @Override
            public AcquireConsumeResult tryAcquire(AcquireConsumeCommand command) {
                ConsumeRecord record = new ConsumeRecord(
                        command.tenantId(),
                        command.consumerGroup(),
                        command.eventId(),
                        command.eventType(),
                        0,
                        command.lockedUntil().plusSeconds(60),
                        command.now(),
                        0,
                        null
                );
                return new AcquireConsumeResult(AcquireConsumeState.IN_PROGRESS, record);
            }

            @Override
            public Optional<ConsumeRecord> find(String consumerGroup, Ulid128 eventId) {
                return Optional.of(new ConsumeRecord(
                        1L, consumerGroup, eventId, "ORDER_CREATED",
                        2, Instant.now(FIXED_CLOCK),
                        Instant.now(FIXED_CLOCK),
                        1, "failed"
                ));
            }
        };
        DefaultEventHandlerTemplate template = newTemplate(repo);
        EventEnvelope envelope = new EventEnvelope(
                1L,
                new Ulid128(9L, 10L),
                "ORDER_CREATED",
                "{}",
                "{}",
                Instant.now(FIXED_CLOCK)
        );

        ConsumeOptions options = new ConsumeOptions(
                Duration.ofSeconds(30),
                true,
                Duration.ofMillis(200),
                20,
                Duration.ofSeconds(1),
                Duration.ofMinutes(5)
        );

        assertThrows(EventConsumeFailedException.class, () ->
                template.consume("ORDER", envelope, evt -> {
                }, options));
    }
}
