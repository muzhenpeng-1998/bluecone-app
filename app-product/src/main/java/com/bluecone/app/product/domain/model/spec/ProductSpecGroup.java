package com.bluecone.app.product.domain.model.spec;

import com.bluecone.app.product.domain.enums.ProductStatus;
import com.bluecone.app.product.domain.enums.SelectType;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品规格组的领域模型，对应 bc_product_spec_group，定义规格维度及选择规则（单选/多选、是否必选等）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpecGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private Long productId;

    private String name;

    private SelectType selectType;

    private boolean required;

    private Integer maxSelect;

    private Integer sortOrder;

    private ProductStatus status;

    private List<ProductSpecOption> options;
}
