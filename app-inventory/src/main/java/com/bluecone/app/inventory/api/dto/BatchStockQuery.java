package com.bluecone.app.inventory.api.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchStockQuery {

    private Long tenantId;

    private Long storeId;

    private List<Long> itemIds;

    private Long locationId;
}
