package com.bluecone.app.core.contextkit;

/**
 * 通用上下文缓存键。
 *
 * @param namespace 逻辑命名空间，例如 store:snap、product:snap
 * @param key       业务主键字符串，例如 tenantId:storeInternalId
 */
public record CacheKey(String namespace, String key) {
}

