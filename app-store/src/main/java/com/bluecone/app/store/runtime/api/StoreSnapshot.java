package com.bluecone.app.store.runtime.api;

/**
 * @deprecated Moved to com.bluecone.app.core.store.StoreSnapshot to avoid circular dependencies.
 * This type alias is kept for backward compatibility.
 */
@Deprecated
public record StoreSnapshot(
        long tenantId,
        com.bluecone.app.id.core.Ulid128 storeInternalId,
        String storePublicId,
        String storeName,
        int status,                 // 1=enabled,0=disabled（按当前门店状态映射）
        boolean openForOrders,      // 配置维度是否允许接单
        String timezone,            // 可选，当前缺省为 null
        long configVersion,         // 配置版本号，用于缓存失效
        java.time.Instant updatedAt,
        java.util.Map<String, Object> ext     // 预留扩展信息，避免频繁改结构
) {
}

