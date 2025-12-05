package com.bluecone.app.product.domain.model;

import com.bluecone.app.product.domain.enums.ProductStatus;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品分类/菜单分组的领域模型，承载菜单树及运营分组信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private Long parentId;

    private String name;

    private Integer type;

    private Integer level;

    private String icon;

    private ProductStatus status;

    private Integer sortOrder;

    private String remark;
}
