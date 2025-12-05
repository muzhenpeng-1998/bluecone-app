package com.bluecone.app.inventory.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockView {

    private Long tenantId;
    private Long storeId;
    private Long itemId;
    private Long locationId;

    private Long totalQty;
    private Long lockedQty;
    private Long availableQty;
    private Long safetyStock;
}
