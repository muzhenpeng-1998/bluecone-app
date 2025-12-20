package com.bluecone.app.product.dto.view;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SKU 视图，包含基本定价与规格组合描述。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSkuView {

    private Long skuId;
    private String skuCode;
    private String name;

    private BigDecimal basePrice;
    private BigDecimal marketPrice;
    private BigDecimal costPrice;

    private String barcode;
    private Boolean isDefault;

    /**
     * 规格组合的 JSON 字符串，保持与领域模型一致，前端可直接解析。
     */
    private String specCombination;

    private Integer status;
    private Integer sortOrder;
}
