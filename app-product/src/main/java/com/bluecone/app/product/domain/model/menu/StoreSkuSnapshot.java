package com.bluecone.app.product.domain.model.menu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店 SKU 快照，提供权威定价与可售信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreSkuSnapshot {

    private Long skuId;

    private Long tenantId;

    private Long storeId;

    private Long productId;

    private String skuName;

    private Long salePrice;

    private Boolean available;
}
