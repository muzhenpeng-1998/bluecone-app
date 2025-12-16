package com.bluecone.app.id.internal.segment;

import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.segment.IdSegmentRepository;
import com.bluecone.app.id.segment.SegmentRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于号段模式的 long 型 ID 生成器。
 * 
 * <p>核心特性：
 * <ul>
 *   <li>本地缓存：每个 scope 缓存一段 ID 范围，快速分配无需访问数据库</li>
 *   <li>并发安全：使用 AtomicLong 保证快路径无锁，号段耗尽时加锁申请新段</li>
 *   <li>高性能：单机 QPS 可达 10 万+</li>
 *   <li>无时钟依赖：避免 Snowflake 的时钟回拨问题</li>
 * </ul>
 * 
 * <p>线程安全性：
 * <ul>
 *   <li>快路径（号段未耗尽）：无锁，使用 AtomicLong.incrementAndGet()</li>
 *   <li>慢路径（号段耗尽）：加锁申请新号段，避免重复申请</li>
 * </ul>
 */
public class SegmentLongIdGenerator {
    
    private static final Logger log = LoggerFactory.getLogger(SegmentLongIdGenerator.class);
    
    /**
     * ID 号段仓储，用于从数据库申请新号段
     */
    private final IdSegmentRepository repository;
    
    /**
     * 号段步长（每次申请的 ID 数量）
     */
    private final int step;
    
    /**
     * 每个 scope 对应的号段缓存
     */
    private final ConcurrentHashMap<IdScope, SegmentBuffer> buffers = new ConcurrentHashMap<>();
    
    /**
     * 构造函数。
     * 
     * @param repository 号段仓储实现
     * @param step 号段步长（建议 1000）
     */
    public SegmentLongIdGenerator(IdSegmentRepository repository, int step) {
        if (repository == null) {
            throw new IllegalArgumentException("repository 不能为 null");
        }
        if (step <= 0) {
            throw new IllegalArgumentException("step 必须大于 0，当前值: " + step);
        }
        this.repository = repository;
        this.step = step;
        log.info("SegmentLongIdGenerator 初始化完成，step={}", step);
    }
    
    /**
     * 生成下一个 long 型 ID。
     * 
     * @param scope 作用域
     * @return 下一个 ID
     * @throws IllegalStateException 如果号段分配失败
     */
    public long nextId(IdScope scope) {
        if (scope == null) {
            throw new IllegalArgumentException("scope 不能为 null");
        }
        
        // 获取或创建 scope 对应的 buffer
        SegmentBuffer buffer = buffers.computeIfAbsent(scope, k -> new SegmentBuffer(scope));
        
        // 快路径：尝试从当前号段分配 ID
        long id = buffer.tryAllocate();
        if (id > 0) {
            return id;
        }
        
        // 慢路径：号段耗尽，加锁申请新号段
        return buffer.allocateWithNewSegment();
    }
    
    /**
     * 号段缓冲区，每个 scope 对应一个。
     */
    private class SegmentBuffer {
        
        private final IdScope scope;
        
        /**
         * 当前号段的游标（下一个待分配的 ID）
         */
        private final AtomicLong cursor = new AtomicLong(0);
        
        /**
         * 当前号段的结束位置（包含）
         */
        private volatile long end = 0;
        
        /**
         * 申请新号段时的锁（避免并发重复申请）
         */
        private final Lock lock = new ReentrantLock();
        
        SegmentBuffer(IdScope scope) {
            this.scope = scope;
        }
        
        /**
         * 尝试从当前号段分配 ID（快路径，无锁）。
         * 
         * @return 分配的 ID，如果号段已耗尽返回 -1
         */
        long tryAllocate() {
            long current = cursor.get();
            if (current > end) {
                return -1; // 号段已耗尽
            }
            
            // 使用 CAS 分配 ID
            long next = cursor.incrementAndGet();
            if (next <= end) {
                return next;
            }
            
            return -1; // 号段已耗尽
        }
        
        /**
         * 申请新号段并分配 ID（慢路径，加锁）。
         * 
         * @return 分配的 ID
         * @throws IllegalStateException 如果号段分配失败
         */
        long allocateWithNewSegment() {
            lock.lock();
            try {
                // 双重检查：可能其他线程已经申请了新号段
                long id = tryAllocate();
                if (id > 0) {
                    return id;
                }
                
                // 从数据库申请新号段
                log.debug("号段耗尽，为 scope={} 申请新号段，step={}", scope, step);
                SegmentRange range = repository.nextRange(scope, step);
                
                // 更新本地缓存：设置 cursor 为 startInclusive - 1，这样下次 incrementAndGet() 会返回 startInclusive
                cursor.set(range.startInclusive() - 1);
                end = range.endInclusive();
                
                log.info("成功申请新号段: scope={}, range=[{}, {}], size={}", 
                         scope, range.startInclusive(), range.endInclusive(), range.size());
                
                // 分配第一个 ID：incrementAndGet() 会先递增（变为 startInclusive），然后返回
                return cursor.incrementAndGet();
                
            } catch (Exception e) {
                log.error("申请号段失败: scope={}", scope, e);
                throw new IllegalStateException("申请号段失败: scope=" + scope, e);
            } finally {
                lock.unlock();
            }
        }
    }
}

