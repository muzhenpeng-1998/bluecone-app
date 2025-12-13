package com.bluecone.app.id.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

/**
 * MonotonicUlidGenerator 行为测试。
 */
class MonotonicUlidGeneratorTest {

    private static int compare(Ulid128 a, Ulid128 b) {
        int msbCmp = Long.compare(a.msb(), b.msb());
        if (msbCmp != 0) {
            return msbCmp;
        }
        return Long.compare(a.lsb(), b.lsb());
    }

    /**
     * 固定时间源（恒定 millis）下生成 10_000 个 ULID，应按 (msb, lsb) 严格递增。
     */
    @Test
    void fixedClockShouldProduceStrictlyIncreasingUlids() {
        MonotonicUlidGenerator generator = new MonotonicUlidGenerator(() -> 1_000L);

        int n = 10_000;
        Ulid128 prev = null;
        for (int i = 0; i < n; i++) {
            Ulid128 current = generator.nextUlid();
            if (prev != null) {
                assertTrue(compare(prev, current) < 0, "ULID 应严格递增");
            }
            prev = current;
        }
    }

    /**
     * 真时间源下生成 200_000 个 ULID，应不存在重复值。
     */
    @Test
    void realClockShouldProduceNoDuplicates() {
        AtomicLong counter = new AtomicLong(System.currentTimeMillis());
        MonotonicUlidGenerator generator = new MonotonicUlidGenerator(counter::getAndIncrement);

        int n = 200_000;
        Set<String> seen = new HashSet<>(n * 2);
        for (int i = 0; i < n; i++) {
            String ulid = generator.nextUlidString();
            assertTrue(seen.add(ulid), "生成的 ULID 不应重复");
        }
        assertEquals(n, seen.size());
    }
}

