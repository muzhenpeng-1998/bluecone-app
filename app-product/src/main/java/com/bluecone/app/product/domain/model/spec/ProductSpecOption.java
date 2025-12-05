package com.bluecone.app.product.domain.model.spec;

import com.bluecone.app.product.domain.enums.ProductStatus;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品规格项的领域模型，对应 bc_product_spec_option，描述具体的规格选项及加价配置。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpecOption implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private Long productId;

    private Long specGroupId;

    private String name;

    private BigDecimal priceDelta;

    private boolean defaultOption;

    private Integer sortOrder;

    private ProductStatus status;
}
