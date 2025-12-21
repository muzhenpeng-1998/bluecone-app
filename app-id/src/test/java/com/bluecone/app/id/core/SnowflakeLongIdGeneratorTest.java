package com.bluecone.app.id.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.bluecone.app.id.internal.core.SnowflakeLongIdGenerator;

/**
 * SnowflakeLongIdGenerator 行为测试。
 */
class SnowflakeLongIdGeneratorTest {

    /**
     * 固定时间源（恒定 millis）下生成 10_000 个 ID，应严格递增且无重复。
     */
    @Test
    void fixedClockShouldProduceStrictlyIncreasingIds() {
        SnowflakeLongIdGenerator generator =
                new SnowflakeLongIdGenerator(0L, 1L, () -> 1_000L);

        int n = 10_000;
        Set<Long> seen = new HashSet<>(n * 2);
        long prev = -1L;
        for (int i = 0; i < n; i++) {
            long id = generator.nextId();
            if (prev >= 0L) {
                assertTrue(id > prev, "Snowflake ID 应严格递增");
            }
            assertTrue(seen.add(id), "Snowflake ID 不应重复");
            prev = id;
        }
        assertEquals(n, seen.size());
    }

    /**
     * nodeId 越界时应抛出 IllegalArgumentException。
     */
    @Test
    void nodeIdOutOfRangeShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new SnowflakeLongIdGenerator(0L, -1L, () -> 0L));
        assertThrows(IllegalArgumentException.class,
                () -> new SnowflakeLongIdGenerator(0L, 1024L, () -> 0L));
    }

    /**
     * 多线程并发生成 100_000 个 ID，应无重复。
     */
    @Test
    void concurrentGenerationShouldProduceUniqueIds() throws InterruptedException {
        long epochMillis = 1704067200000L; // 2024-01-01
        SnowflakeLongIdGenerator generator = new SnowflakeLongIdGenerator(epochMillis, 5L);

        int threads = 10;
        int idsPerThread = 10_000;
        Set<Long> allIds = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            new Thread(() -> {
                for (int i = 0; i < idsPerThread; i++) {
                    long id = generator.nextId();
                    assertTrue(id > 0, "ID 应为正数");
                    allIds.add(id);
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        assertEquals(threads * idsPerThread, allIds.size(), "所有 ID 应唯一不重复");
    }

    /**
     * 时钟回拨场景：时间源返回更小的值时，应使用上一次时间戳，保证单调性。
     */
    @Test
    void clockRollbackShouldNotProduceDuplicateIds() {
        AtomicLong currentTime = new AtomicLong(1000L);
        SnowflakeLongIdGenerator generator =
                new SnowflakeLongIdGenerator(0L, 1L, currentTime::get);

        // 生成第一个 ID
        long id1 = generator.nextId();
        assertTrue(id1 > 0);

        // 时钟回拨 100ms
        currentTime.set(900L);

        // 生成第二个 ID，应仍然单调递增
        long id2 = generator.nextId();
        assertTrue(id2 > id1, "时钟回拨后 ID 仍应单调递增");

        // 恢复正常时间
        currentTime.set(1100L);

        // 生成第三个 ID
        long id3 = generator.nextId();
        assertTrue(id3 > id2);
    }

    /**
     * 同一毫秒内序列号溢出时，应推进时间戳 +1ms。
     */
    @Test
    void sequenceOverflowShouldAdvanceTimestamp() {
        SnowflakeLongIdGenerator generator =
                new SnowflakeLongIdGenerator(0L, 1L, () -> 1000L);

        // 生成 4096 个 ID（超过 12 位序列号的最大值 4095）
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            long id = generator.nextId();
            assertTrue(ids.add(id), "序列号溢出后 ID 仍应唯一");
        }
    }

    /**
     * 时间戳超出 41 位范围时应抛异常。
     */
    @Test
    void timestampOverflowShouldThrowException() {
        long maxTimestamp = (1L << 41) - 1L;
        long overflowTime = maxTimestamp + 1000L;

        SnowflakeLongIdGenerator generator =
                new SnowflakeLongIdGenerator(0L, 1L, () -> overflowTime);

        assertThrows(IllegalStateException.class, generator::nextId,
                "时间戳超出 41 位范围应抛出 IllegalStateException");
    }
}

