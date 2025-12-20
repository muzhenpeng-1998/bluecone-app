package com.bluecone.app.product.dto.view;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规格组视图，携带规格项信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpecGroupView {

    private Long specGroupId;
    private String name;
    private Integer selectType;
    private Boolean required;
    private Integer maxSelect;
    private Integer sortOrder;

    private Integer status;

    private List<ProductSpecOptionView> options;
}
