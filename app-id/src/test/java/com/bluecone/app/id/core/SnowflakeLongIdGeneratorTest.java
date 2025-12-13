package com.bluecone.app.id.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

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
}

