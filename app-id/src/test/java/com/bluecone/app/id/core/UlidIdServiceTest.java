package com.bluecone.app.id.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.bluecone.app.id.api.IdService;

/**
 * UlidIdService 相关单元测试。
 */
class UlidIdServiceTest {

    private IdService newIdService() {
        UlidIdGenerator generator = new UlidIdGenerator();
        return new UlidIdService(generator);
    }

    /**
     * 测试字节长度与可逆性：Ulid128 -> bytes -> Ulid128 -> String。
     */
    @Test
    void bytesLengthAndReversibility() {
        IdService idService = newIdService();

        Ulid128 ulid = idService.nextUlid();
        byte[] bytes = ulid.toBytes();

        assertNotNull(bytes);
        assertEquals(16, bytes.length, "ULID 字节长度必须为 16");

        Ulid128 restored = Ulid128.fromBytes(bytes);
        assertEquals(ulid.msb(), restored.msb());
        assertEquals(ulid.lsb(), restored.lsb());

        String restoredString = restored.toString();
        assertNotNull(restoredString);
        assertEquals(26, restoredString.length(), "还原后的字符串长度必须为 26");
    }

    /**
     * 测试并发唯一性：32 线程 * 每线程 5000，共 160k。
     */
    @Test
    void concurrentUniqueness() throws InterruptedException {
        IdService idService = newIdService();

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
                        String ulid = idService.nextUlidString();
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

        assertTrue(completed, "并发生成 ULID 超时");
        assertEquals(total, all.size(), "并发生成的 ULID 应全部唯一");
    }

    /**
     * 基本格式校验：长度为 26，且只包含大写字母与数字。
     */
    @Test
    void ulidStringBasicFormat() {
        IdService idService = newIdService();

        String ulid = idService.nextUlidString();

        assertNotNull(ulid);
        assertEquals(26, ulid.length(), "ULID 字符串长度必须为 26");
        assertTrue(ulid.matches("[0-9A-Z]{26}"), "ULID 字符串格式不符合预期: " + ulid);
    }
}

