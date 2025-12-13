package com.bluecone.app.core.contextkit;

/**
 * 快照序列化适配器，将快照对象转换为可缓存值，或从缓存值还原为快照。
 */
public interface SnapshotSerde<T> {

    /**
     * 将快照转换为缓存存储的值（可为原对象或 Map 等）。
     */
    Object toCacheValue(T value);

    /**
     * 从缓存中的值还原为快照对象。
     */
    T fromCacheValue(Object cached);
}

