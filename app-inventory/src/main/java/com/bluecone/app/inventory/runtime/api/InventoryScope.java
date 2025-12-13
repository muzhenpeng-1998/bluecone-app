package com.bluecone.app.inventory.runtime.api;

import com.bluecone.app.id.core.Ulid128;

/**
 * 库存策略快照加载 scope：聚合门店内部 ID、公有 ID 与数值型 ID。
 */
public record InventoryScope(
        Ulid128 storeInternalId,
        String storePublicId,
        Long storeNumericId
) {

    @Override
    public String toString() {
        if (storeInternalId != null) {
            return storeInternalId.toString();
        }
        if (storeNumericId != null) {
            return String.valueOf(storeNumericId);
        }
        return "unknown";
    }
}

