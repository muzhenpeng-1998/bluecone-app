package com.bluecone.app.product.domain.model.menu;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 菜单商品视图，承载前端需要展示的核心字段（SPU 级别）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreMenuProductView implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long productId;
    private String name;
    private String subtitle;
    private String mainImage;
    private List<String> tags;
    private Map<String, Object> productMeta;

    private List<StoreMenuSkuView> skus;
    private List<StoreMenuSpecGroupView> specGroups;
    private List<StoreMenuAttrGroupView> attrGroups;
    private List<StoreMenuAddonGroupView> addonGroups;
}
