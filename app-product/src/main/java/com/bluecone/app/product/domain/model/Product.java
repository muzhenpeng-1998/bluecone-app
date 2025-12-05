package com.bluecone.app.product.domain.model;

import com.bluecone.app.product.domain.enums.ProductStatus;
import com.bluecone.app.product.domain.enums.ProductType;
import com.bluecone.app.product.domain.model.addon.AddonGroup;
import com.bluecone.app.product.domain.model.attribute.ProductAttributeGroup;
import com.bluecone.app.product.domain.model.spec.ProductSpecGroup;
import com.bluecone.app.product.domain.model.tag.ProductTag;
import com.bluecone.app.product.domain.model.store.ProductStoreConfig;
import com.bluecone.app.product.domain.model.ProductCategory;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品聚合根，表达商品 SPU 及其规格、属性、小料、标签等聚合视图。
 * <p>面向领域层和应用层的核心入口，屏蔽底层多表结构（SPU/SKU/规格/属性/小料/标签/门店配置）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private String productCode;

    private String name;

    private String subtitle;

    private ProductType productType;

    private String description;

    private String mainImage;

    private List<String> mediaGallery;

    private String unit;

    private ProductStatus status;

    private Integer sortOrder;

    private Map<String, Object> productMeta;

    private List<ProductSku> skus;

    private List<ProductSpecGroup> specGroups;

    private List<ProductAttributeGroup> attributeGroups;

    private List<AddonGroup> addonGroups;

    private List<ProductTag> tags;

    private List<ProductCategory> categories;

    private List<ProductStoreConfig> storeConfigs;
}
