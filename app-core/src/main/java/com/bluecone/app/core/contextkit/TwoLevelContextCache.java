package com.bluecone.app.core.contextkit;

import java.time.Duration;
import java.util.Optional;

/**
 * 简单的两级缓存组合器：优先访问 L1，miss 后访问 L2。
 */
public class TwoLevelContextCache implements ContextCache {

    private final ContextCache l1;
    private final ContextCache l2;

    public TwoLevelContextCache(ContextCache l1, ContextCache l2) {
        this.l1 = l1;
        this.l2 = l2;
    }

    @Override
    public <T> Optional<CacheValue> get(CacheKey key) {
        Optional<CacheValue> v1 = l1.get(key);
        if (v1.isPresent()) {
            return v1;
        }
        Optional<CacheValue> v2 = l2.get(key);
        v2.ifPresent(value -> {
            // 回填 L1，TTL 由调用方在后续 put 时控制；此处不设置 TTL。
        });
        return v2;
    }

    @Override
    public void put(CacheKey key, CacheValue value, Duration ttl) {
        l1.put(key, value, ttl);
        l2.put(key, value, ttl);
    }

    @Override
    public void invalidate(CacheKey key) {
        l1.invalidate(key);
        l2.invalidate(key);
    }
}

