package com.bluecone.app.product.dto.view;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品详情视图（SPU + SKU + 规格/属性/小料/分类），仅用于读场景。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailView {

    private Long productId;
    private Long tenantId;

    private String productCode;
    private String name;
    private String subtitle;
    private Integer productType;
    private String description;

    private String mainImage;
    private List<String> mediaGallery;

    private String unit;
    private Integer status;
    private Integer sortOrder;

    private List<ProductCategoryBriefView> categories;

    private List<ProductSkuView> skus;

    private List<ProductSpecGroupView> specGroups;

    private List<ProductAttrGroupView> attrGroups;

    private List<ProductAddonGroupView> addonGroups;
}
