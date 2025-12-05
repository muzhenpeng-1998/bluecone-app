package com.bluecone.app.product.domain.model.attribute;

import com.bluecone.app.product.domain.enums.ProductStatus;
import com.bluecone.app.product.domain.enums.SelectType;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用属性组领域模型，对应 bc_product_attr_group，可在租户层复用到多个商品（如甜度、做法）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private String name;

    private Integer scope;

    private SelectType selectType;

    private boolean required;

    private Integer maxSelect;

    private Integer sortOrder;

    private String remark;

    private ProductStatus status;

    private List<ProductAttributeOption> options;
}
