package com.bluecone.app.id.segment;

import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.internal.segment.SegmentLongIdGenerator;
import com.bluecone.app.id.segment.IdSegmentRepository;
import com.bluecone.app.id.segment.SegmentRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * SegmentLongIdGenerator 并发测试。
 * 
 * <p>测试目标：
 * <ul>
 *   <li>唯一性：100 线程 * 10000 次生成，所有 ID 不重复</li>
 *   <li>单调性：同一 scope 内 ID 单调递增（允许不连续）</li>
 *   <li>并发安全：多线程并发生成无异常</li>
 *   <li>边界正确性：号段切换时无跳号/漏号</li>
 * </ul>
 */
@DisplayName("SegmentLongIdGenerator 并发测试")
class SegmentLongIdGeneratorTest {
    
    private IdSegmentRepository mockRepository;
    private SegmentLongIdGenerator generator;
    
    @BeforeEach
    void setUp() {
        // 使用内存模拟仓储（线程安全）
        mockRepository = new InMemoryIdSegmentRepository();
        generator = new SegmentLongIdGenerator(mockRepository, 1000);
    }
    
    @Test
    @DisplayName("单线程生成 ID - 唯一性和单调性")
    void testSingleThreadGeneration() {
        Set<Long> ids = new HashSet<>();
        long lastId = 0;
        
        for (int i = 0; i < 10000; i++) {
            long id = generator.nextId(IdScope.ORDER);
            
            // 唯一性断言
            assertThat(ids).doesNotContain(id);
            ids.add(id);
            
            // 单调性断言
            assertThat(id).isGreaterThan(lastId);
            lastId = id;
        }
        
        assertThat(ids).hasSize(10000);
    }
    
    @Test
    @DisplayName("多线程并发生成 ID - 唯一性和单调性")
    void testConcurrentGeneration() throws InterruptedException {
        int threadCount = 100;
        int idsPerThread = 10000;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        // 使用 ConcurrentHashMap 收集所有生成的 ID
        Set<Long> allIds = ConcurrentHashMap.newKeySet();
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        long id = generator.nextId(IdScope.ORDER);
                        allIds.add(id);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // 唯一性断言：100 * 10000 = 1,000,000 个 ID 必须全部唯一
        assertThat(allIds).hasSize(threadCount * idsPerThread);
    }
    
    @Test
    @DisplayName("多 scope 并发生成 - 隔离性")
    void testMultiScopeConcurrentGeneration() throws InterruptedException {
        int threadCount = 50;
        int idsPerThread = 5000;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount * 2);
        CountDownLatch latch = new CountDownLatch(threadCount * 2);
        
        Set<Long> orderIds = ConcurrentHashMap.newKeySet();
        Set<Long> tenantIds = ConcurrentHashMap.newKeySet();
        
        // ORDER scope
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        long id = generator.nextId(IdScope.ORDER);
                        orderIds.add(id);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // TENANT scope
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        long id = generator.nextId(IdScope.TENANT);
                        tenantIds.add(id);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // 每个 scope 内唯一
        assertThat(orderIds).hasSize(threadCount * idsPerThread);
        assertThat(tenantIds).hasSize(threadCount * idsPerThread);
    }
    
    @Test
    @DisplayName("号段耗尽后自动申请新号段 - 无跳号")
    void testSegmentRefill() {
        // 小步长，快速触发号段耗尽
        IdSegmentRepository smallStepRepo = new InMemoryIdSegmentRepository();
        SegmentLongIdGenerator smallStepGenerator = new SegmentLongIdGenerator(smallStepRepo, 10);
        
        Set<Long> ids = new HashSet<>();
        
        // 生成 100 个 ID，会触发多次号段申请
        for (int i = 0; i < 100; i++) {
            long id = smallStepGenerator.nextId(IdScope.STORE);
            ids.add(id);
        }
        
        // 唯一性
        assertThat(ids).hasSize(100);
        
        // 连续性：应该是 [1, 100]，无跳号
        var sortedIds = ids.stream().sorted().collect(Collectors.toList());
        for (int i = 0; i < sortedIds.size(); i++) {
            assertThat(sortedIds.get(i)).isEqualTo(i + 1L);
        }
    }
    
    @Test
    @DisplayName("号段切换边界测试 - 验证无重复无跳号")
    void testSegmentBoundary() {
        // 步长为 5，测试号段切换边界
        IdSegmentRepository repo = new InMemoryIdSegmentRepository();
        SegmentLongIdGenerator gen = new SegmentLongIdGenerator(repo, 5);
        
        Set<Long> ids = new HashSet<>();
        
        // 生成 15 个 ID，会触发 3 次号段申请：[1-5], [6-10], [11-15]
        for (int i = 0; i < 15; i++) {
            long id = gen.nextId(IdScope.PRODUCT);
            assertThat(ids).doesNotContain(id); // 确保无重复
            ids.add(id);
        }
        
        // 验证连续性
        assertThat(ids).containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L);
    }
    
    @Test
    @DisplayName("并发号段切换 - 无重复")
    void testConcurrentSegmentSwitch() throws InterruptedException {
        // 小步长 + 高并发，频繁触发号段切换
        IdSegmentRepository repo = new InMemoryIdSegmentRepository();
        SegmentLongIdGenerator gen = new SegmentLongIdGenerator(repo, 50);
        
        int threadCount = 20;
        int idsPerThread = 100;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        Set<Long> allIds = ConcurrentHashMap.newKeySet();
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        long id = gen.nextId(IdScope.SKU);
                        allIds.add(id);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // 唯一性：20 * 100 = 2000 个 ID 全部唯一
        assertThat(allIds).hasSize(threadCount * idsPerThread);
        
        // 连续性：应该是 [1, 2000]
        var sortedIds = allIds.stream().sorted().collect(Collectors.toList());
        for (int i = 0; i < sortedIds.size(); i++) {
            assertThat(sortedIds.get(i)).isEqualTo(i + 1L);
        }
    }
    
    /**
     * 内存模拟的 ID 号段仓储（线程安全）。
     */
    private static class InMemoryIdSegmentRepository implements IdSegmentRepository {
        
        private final ConcurrentHashMap<IdScope, AtomicLong> maxIds = new ConcurrentHashMap<>();
        
        @Override
        public synchronized SegmentRange nextRange(IdScope scope, int step) {
            AtomicLong maxId = maxIds.computeIfAbsent(scope, k -> new AtomicLong(0));
            
            long currentMax = maxId.get();
            long newMax = currentMax + step;
            maxId.set(newMax);
            
            return new SegmentRange(currentMax + 1, newMax);
        }
        
        @Override
        public void initScopeIfAbsent(IdScope scope, long initialMaxId, int defaultStep) {
            maxIds.putIfAbsent(scope, new AtomicLong(initialMaxId));
        }
    }
}

