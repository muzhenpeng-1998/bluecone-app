package com.bluecone.app.infra.cache.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine 封装的 L1 缓存，偏向低延迟热点保护。
 */
public class L1CacheStore implements CacheStore {

    private final Cache<CacheKey, CacheEntry> cache;

    public L1CacheStore(long maximumSize) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                // 兜底过期，真正 TTL 由 CacheEntry 控制，避免遗留条目
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
    }

    @Override
    public <T> Optional<T> get(CacheKey key, Class<T> type) {
        CacheEntry entry = cache.getIfPresent(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired()) {
            cache.invalidate(key);
            return Optional.empty();
        }
        return entry.value().asType(type);
    }

    @Override
    public void put(CacheKey key, Object value, Duration ttl) {
        cache.put(key, new CacheEntry(CacheValueWrapper.of(value), ttl));
    }

    @Override
    public void evict(CacheKey key) {
        cache.invalidate(key);
    }

    private record CacheEntry(CacheValueWrapper value, Instant expireAt) {
        CacheEntry(CacheValueWrapper value, Duration ttl) {
            this(value, Instant.now().plus(ttl));
        }

        boolean isExpired() {
            return Instant.now().isAfter(expireAt);
        }
    }
}
