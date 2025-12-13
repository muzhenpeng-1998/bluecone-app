package com.bluecone.app.id.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import de.huxhorn.sulky.ulid.ULID;

/**
 * UlidIdGenerator STRIPED 模式相关测试。
 */
class UlidStripedModeTest {

    /**
     * STRIPED 模式下的并发唯一性测试：
     * stripes=32，32 线程并发生成 160k 个 ULID，要求全部唯一。
     */
    @Test
    void stripedModeConcurrentUniqueness() throws InterruptedException {
        UlidIdGenerator generator = UlidIdGenerator.create(32);

        int threads = 32;
        int perThread = 5000;
        int total = threads * perThread;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        Set<String> all = ConcurrentHashMap.newKeySet();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < perThread; j++) {
                        String ulid = generator.nextUlid();
                        all.add(ulid);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertTrue(completed, "STRIPED 模式并发生成 ULID 超时");
        assertEquals(total, all.size(), "STRIPED 模式并发生成的 ULID 应全部唯一");
    }

    /**
     * 验证 stripes=16 时，stripeBits 编码到 lsb 低位生效：
     * 低 4 位应始终等于线程映射到的分片索引。
     */
    @Test
    void stripeBitsEncodedIntoLeastSignificantBits() {
        int stripes = 16;
        UlidIdGenerator generator = UlidIdGenerator.create(stripes);

        long threadId = Thread.currentThread().getId();
        int idx = (stripes == 1) ? 0 : (int) (threadId % stripes);
        if (idx < 0) {
            idx += stripes;
        }

        long mask = (1L << 4) - 1L; // stripes=16 => stripeBits=4
        long expected = idx & mask;

        for (int i = 0; i < 100; i++) {
            ULID.Value value = generator.nextValue();
            long lsb = value.getLeastSignificantBits();
            long actual = lsb & mask;
            assertEquals(expected, actual, "stripeId 未正确编码到 lsb 低位");
        }
    }
}

