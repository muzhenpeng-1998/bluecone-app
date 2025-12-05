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

    private String requestId;

    private AdjustType adjustType;

    public enum AdjustType {
        INBOUND,
        OUTBOUND,
        INVENTORY_CHECK
    }
}
