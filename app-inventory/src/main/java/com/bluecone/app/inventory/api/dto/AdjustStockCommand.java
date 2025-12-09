package com.bluecone.app.inventory.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustStockCommand {

    private Long tenantId;

    private Long storeId;

    private Long itemId;

    private Long locationId;

    private Long qty;

    private String bizRefType;

    private Long bizRefId;

    /**
     * 操作来源，例如 MANUAL_ADJUST / STOCK_TAKING。
     */
    private String source;

    /**
     * 操作人 ID，便于审计。
     */
    private Long operatorId;

    private String requestId;

    private AdjustType adjustType;

    public enum AdjustType {
        INBOUND,
        OUTBOUND,
        INVENTORY_CHECK
    }
}
