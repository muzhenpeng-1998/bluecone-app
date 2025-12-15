package com.bluecone.app.core.store;

import com.bluecone.app.id.core.Ulid128;

import java.time.Instant;
import java.util.Map;

/**
 * 门店运行态快照（Store 维度），用于网关/业务在进入核心逻辑前快速判断门店状态。
 *
 * <p>不直接暴露底层表结构，只保留跨模块稳定字段。</p>
 */
public record StoreSnapshot(
        long tenantId,
        Ulid128 storeInternalId,
        String storePublicId,
        String storeName,
        int status,                 // 1=enabled,0=disabled（按当前门店状态映射）
        boolean openForOrders,      // 配置维度是否允许接单
        String timezone,            // 可选，当前缺省为 null
        long configVersion,         // 配置版本号，用于缓存失效
        Instant updatedAt,
        Map<String, Object> ext     // 预留扩展信息，避免频繁改结构
) {
}

