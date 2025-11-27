package com.bluecone.app.infra.cache.core;

import java.time.Duration;
import java.util.Optional;

/**
 * 关闭 L2 时的空实现，避免在无 Redis 环境下抛异常。
 */
public class NoopCacheStore implements CacheStore {
    @Override
    public <T> Optional<T> get(CacheKey key, Class<T> type) {
        return Optional.empty();
    }

    @Override
    public void put(CacheKey key, Object value, Duration ttl) {
        // no-op
    }

    @Override
    public void evict(CacheKey key) {
        // no-op
    }
}
