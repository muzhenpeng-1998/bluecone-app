package com.bluecone.app.product.dto.view;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小料组视图，包含计价/不计价小料及排序信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAddonGroupView {

    private Long addonGroupId;
    private String name;
    private Integer type;
    private Integer sortOrder;
    private String remark;

    private Boolean required;
    private String maxTotalQuantity;

    private List<ProductAddonItemView> items;
}
