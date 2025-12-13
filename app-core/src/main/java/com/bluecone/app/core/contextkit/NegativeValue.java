package com.bluecone.app.core.contextkit;

/**
 * 负缓存标记，用于防止高并发下对不存在数据的重复访问。
 *
 * @param reason 负缓存原因说明（可选）
 */
public record NegativeValue(String reason) implements CacheValue {
}

