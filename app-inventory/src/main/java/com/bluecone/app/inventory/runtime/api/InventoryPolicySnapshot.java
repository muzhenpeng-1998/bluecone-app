package com.bluecone.app.inventory.runtime.api;

import com.bluecone.app.id.core.Ulid128;

import java.time.Instant;
import java.util.Map;

/**
 * 库存策略快照（门店维度，只读）。
 *
 * <p>用于下单链路在进入核心库存扣减前，快速获取当前门店的库存策略配置。</p>
 */
public record InventoryPolicySnapshot(
        long tenantId,
        Ulid128 storeId,
        String storePublicId,
        long configVersion,
        boolean enableInventory,
        String deductMode,
        int safetyStockMode,
        Instant updatedAt,
        Map<String, Object> ext
) {
}

