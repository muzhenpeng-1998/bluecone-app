package com.bluecone.app.core.contextkit;

import java.time.Duration;
import java.util.Optional;

/**
 * 通用上下文缓存接口。L1 必须，L2 可选。
 */
public interface ContextCache {

    /**
     * 获取缓存值。
     */
    <T> Optional<CacheValue> get(CacheKey key);

    /**
     * 写入缓存值。
     */
    void put(CacheKey key, CacheValue value, Duration ttl);

    /**
     * 失效缓存。
     */
    default void invalidate(CacheKey key) {
        // 默认无操作，由具体实现选择性支持
    }
}

