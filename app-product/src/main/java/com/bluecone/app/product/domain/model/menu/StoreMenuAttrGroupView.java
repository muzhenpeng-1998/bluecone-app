package com.bluecone.app.product.domain.model.menu;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 菜单属性组视图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreMenuAttrGroupView implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long attrGroupId;
    private String name;
    private Integer scope;
    private Integer selectType;
    private Boolean required;
    private Integer maxSelect;
    private Integer sortOrder;
    private List<StoreMenuAttrOptionView> options;
}
