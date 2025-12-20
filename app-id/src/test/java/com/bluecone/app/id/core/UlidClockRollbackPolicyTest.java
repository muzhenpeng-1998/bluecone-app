package com.bluecone.app.id.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.bluecone.app.id.internal.config.BlueconeIdProperties;
import com.bluecone.app.id.internal.config.BlueconeIdProperties.Ulid.Rollback;
import com.bluecone.app.id.internal.metrics.UlidMetrics;

import de.huxhorn.sulky.ulid.ULID;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * UlidIdGenerator 时钟回拨策略与指标测试。
 */
class UlidClockRollbackPolicyTest {

    /**
     * 可变时钟实现，便于在测试中模拟回拨场景。
     */
    static class MutableClock extends Clock {

        private final AtomicLong millis = new AtomicLong();
        private final ZoneId zone;

        MutableClock(long initialMillis) {
            this.zone = ZoneId.of("UTC");
            this.millis.set(initialMillis);
        }

        void setMillis(long newMillis) {
            this.millis.set(newMillis);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis.get());
        }
    }

    private long extractTimestamp(ULID.Value value) {
        long msb = value.getMostSignificantBits();
        return (msb >>> 16) & 0x0000_FFFF_FFFF_FFFFL;
    }

    /**
     * USE_LAST 策略：回拨时继续使用上一次时间戳，保证单调，
     * 且会记录回拨指标。
     */
    @Test
    void useLastPolicyKeepsTimestampMonotonicAndRecordsMetrics() {
        MutableClock clock = new MutableClock(1_000L);

        Rollback rollback = new Rollback();
        rollback.setPolicy(BlueconeIdProperties.Ulid.Policy.USE_LAST);

        MeterRegistry registry = new SimpleMeterRegistry();
        UlidMetrics metrics = UlidMetrics.from(
                registry,
                "bluecone.id.ulid",
                1,
                BlueconeIdProperties.Mode.STRICT
        );

        com.bluecone.app.id.internal.core.UlidIdGenerator generator = com.bluecone.app.id.internal.core.UlidIdGenerator.create(1, clock, rollback, metrics);

        ULID.Value first = generator.nextValue();
        long ts1 = extractTimestamp(first);

        // 模拟时钟回拨 100ms
        clock.setMillis(900L);
        ULID.Value second = generator.nextValue();
        long ts2 = extractTimestamp(second);

        // 第二次的时间戳不应小于第一次，应等于上一次（USE_LAST）
        assertTrue(ts2 >= ts1);
        assertEquals(ts1, ts2);

        double rollbackCount = registry
                .get("bluecone.id.ulid.rollback.total")
                .tag("mode", "STRICT")
                .counter()
                .count();
        assertEquals(1.0, rollbackCount, 0.000001);
    }

    /**
     * FAIL_FAST 策略：当回拨幅度超过阈值时抛出异常。
     */
    @Test
    void failFastPolicyThrowsOnLargeRollback() {
        MutableClock clock = new MutableClock(1_000L);

        Rollback rollback = new Rollback();
        rollback.setPolicy(BlueconeIdProperties.Ulid.Policy.FAIL_FAST);
        rollback.setFailFastThresholdMs(10L);

        com.bluecone.app.id.internal.core.UlidIdGenerator generator = com.bluecone.app.id.internal.core.UlidIdGenerator.create(
                1,
                clock,
                rollback,
                UlidMetrics.noop()
        );

        // 第一次生成，记录基准时间戳
        generator.nextValue();

        // 回拨 100ms，超过阈值 10ms，触发 FAIL_FAST
        clock.setMillis(900L);

        assertThrows(ClockRollbackException.class, generator::nextValue);
    }

    /**
     * 指标生成计数测试：生成 N 次，generated.total 与 Timer 计数应 >= N。
     */
    @Test
    void metricsCountGeneratedUlids() {
        MutableClock clock = new MutableClock(1_000L);

        Rollback rollback = new Rollback();
        rollback.setPolicy(BlueconeIdProperties.Ulid.Policy.USE_LAST);

        MeterRegistry registry = new SimpleMeterRegistry();
        UlidMetrics metrics = UlidMetrics.from(
                registry,
                "bluecone.id.ulid",
                1,
                BlueconeIdProperties.Mode.STRICT
        );

        com.bluecone.app.id.internal.core.UlidIdGenerator generator = com.bluecone.app.id.internal.core.UlidIdGenerator.create(
                1,
                clock,
                rollback,
                metrics
        );

        int n = 10;
        for (int i = 0; i < n; i++) {
            generator.nextValue();
        }

        double generatedCount = registry
                .get("bluecone.id.ulid.generated.total")
                .tag("mode", "STRICT")
                .counter()
                .count();
        assertTrue(generatedCount >= n);

        Timer timer = registry
                .get("bluecone.id.ulid.generate.latency")
                .tag("mode", "STRICT")
                .timer();
        assertEquals(n, timer.count());
    }
}

