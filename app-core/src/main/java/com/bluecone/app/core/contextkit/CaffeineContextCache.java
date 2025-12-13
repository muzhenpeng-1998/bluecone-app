package com.bluecone.app.core.contextkit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 基于 Caffeine 的 L1 上下文缓存实现。
 */
public class CaffeineContextCache implements ContextCache {

    private final Cache<CacheKey, Entry> cache;

    public CaffeineContextCache(long maximumSize) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .build();
    }

    @Override
    public <T> Optional<CacheValue> get(CacheKey key) {
        Entry entry = cache.getIfPresent(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired()) {
            cache.invalidate(key);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public void put(CacheKey key, CacheValue value, Duration ttl) {
        cache.put(key, new Entry(value, ttl));
    }

    private static final class Entry {
        private final CacheValue value;
        private final Instant expireAt;

        private Entry(CacheValue value, Duration ttl) {
            this.value = value;
            this.expireAt = Instant.now().plus(ttl);
        }

        private CacheValue value() {
            return value;
        }

        private boolean isExpired() {
            return Instant.now().isAfter(expireAt);
        }
    }
}

