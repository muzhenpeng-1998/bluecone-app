package com.bluecone.app.product.domain.model.attribute;

import com.bluecone.app.product.domain.enums.ProductStatus;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 属性项领域模型，对应 bc_product_attr_option，承载可复用的属性值及加价配置。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeOption implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private Long attrGroupId;

    private String name;

    private String valueCode;

    private BigDecimal priceDelta;

    private Integer sortOrder;

    private ProductStatus status;
}
