package com.bluecone.app.product.domain.model.addon;

import com.bluecone.app.product.domain.enums.ProductStatus;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小料项领域模型，对应 bc_addon_item，描述小料的定价与可选数量规则。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddonItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private Long groupId;

    private String name;

    private BigDecimal price;

    private BigDecimal maxQuantity;

    private BigDecimal freeLimit;

    private Integer sortOrder;

    private ProductStatus status;
}
