package com.bluecone.app.core.idempotency.spi;

import java.time.Duration;

/**
 * 幂等锁接口，通常由 Redis 实现，用于减少数据库抢锁压力。
 *
 * <p>注意：锁只是加速组件，最终一致性必须依赖数据库。</p>
 */
public interface IdempotencyLock {

    /**
     * 尝试在指定 TTL 内获取锁。
     *
     * @param key 锁键
     * @param ttl 锁过期时间
     * @return true 表示获取成功
     */
    boolean tryLock(String key, Duration ttl);

    /**
     * 释放锁。
     *
     * @param key 锁键
     */
    void unlock(String key);

    /**
     * 返回一个空实现，所有操作均为 no-op。
     */
    static IdempotencyLock noop() {
        return new IdempotencyLock() {
            @Override
            public boolean tryLock(String key, Duration ttl) {
                return true;
            }

            @Override
            public void unlock(String key) {
                // no-op
            }
        };
    }
}

