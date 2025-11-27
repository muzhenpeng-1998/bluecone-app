package com.bluecone.app.infra.cache.core;

import java.time.Duration;
import java.util.Optional;

/**
 * Minimal cache store contract so内核可以自由组合 L1/L2。
 */
public interface CacheStore {

    <T> Optional<T> get(CacheKey key, Class<T> type);

    void put(CacheKey key, Object value, Duration ttl);

    void evict(CacheKey key);
}
