package com.bluecone.app.product.domain.model;

import com.bluecone.app.product.domain.enums.ProductStatus;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品 SKU 领域模型，表示 SPU 下的具体可售单元（定价、条码、规格组合等）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSku implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private Long productId;

    private String skuCode;

    private String name;

    private BigDecimal basePrice;

    private BigDecimal marketPrice;

    private BigDecimal costPrice;

    private String barcode;

    private boolean defaultSku;

    private Map<String, Object> specCombination;

    private ProductStatus status;

    private Integer sortOrder;

    private Map<String, Object> skuMeta;
}
