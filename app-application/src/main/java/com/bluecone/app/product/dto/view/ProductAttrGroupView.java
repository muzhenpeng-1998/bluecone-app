package com.bluecone.app.product.dto.view;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 属性组视图，描述口味/做法等可复用属性组。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttrGroupView {

    private Long attrGroupId;
    private String name;
    private Integer scope;
    private Integer selectType;
    private Boolean required;
    private Integer maxSelect;
    private Integer sortOrder;
    private Integer status;

    private List<ProductAttrOptionView> options;
}
