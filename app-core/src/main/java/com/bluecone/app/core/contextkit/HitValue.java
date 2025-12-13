package com.bluecone.app.core.contextkit;

/**
 * 正向缓存命中，携带值与版本号。
 *
 * @param value   实际缓存值（可为领域快照或序列化映射）
 * @param version 当前快照版本号
 */
public record HitValue<T>(T value, long version) implements CacheValue {
}

