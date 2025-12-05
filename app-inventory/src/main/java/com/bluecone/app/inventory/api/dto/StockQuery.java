package com.bluecone.app.inventory.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockQuery {

    private Long tenantId;

    private Long storeId;

    private Long itemId;

    private Long locationId;
}
