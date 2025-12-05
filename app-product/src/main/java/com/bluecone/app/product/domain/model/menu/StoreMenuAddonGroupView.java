package com.bluecone.app.product.domain.model.menu;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 菜单小料组视图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreMenuAddonGroupView implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long addonGroupId;
    private String name;
    private Integer type;
    private Integer sortOrder;
    private String remark;
    private Boolean required;
    private String maxTotalQuantity;
    private List<StoreMenuAddonItemView> items;
}
