package com.bluecone.app.inventory.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockStockCommand {

    private Long tenantId;

    private Long storeId;

    /**
     * 库存对象 ID，对应 InventoryItem.id。
     */
    private Long itemId;

    /**
     * 库位 ID，可为空，空时默认 0。
     */
    private Long locationId;

    private Long orderId;

    private Long orderItemId;

    /**
     * 本次锁定数量（最小单位）。
     */
    private Long lockQty;

    /**
     * 幂等请求 ID。
     */
    private String requestId;

    /**
     * 锁的过期时长（秒），为空则走默认配置。
     */
    private Integer lockExpireSeconds;
}
