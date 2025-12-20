package com.bluecone.app.product.application.query;

import com.bluecone.app.product.domain.model.Product;
import com.bluecone.app.product.domain.model.ProductCategory;
import com.bluecone.app.product.domain.model.ProductSku;
import com.bluecone.app.product.domain.model.addon.AddonGroup;
import com.bluecone.app.product.domain.model.addon.AddonItem;
import com.bluecone.app.product.domain.model.attribute.ProductAttributeGroup;
import com.bluecone.app.product.domain.model.attribute.ProductAttributeOption;
import com.bluecone.app.product.domain.model.spec.ProductSpecGroup;
import com.bluecone.app.product.domain.model.spec.ProductSpecOption;
import com.bluecone.app.product.domain.repository.ProductRepository;
import com.bluecone.app.product.dto.view.ProductAddonGroupView;
import com.bluecone.app.product.dto.view.ProductAddonItemView;
import com.bluecone.app.product.dto.view.ProductAttrGroupView;
import com.bluecone.app.product.dto.view.ProductAttrOptionView;
import com.bluecone.app.product.dto.view.ProductCategoryBriefView;
import com.bluecone.app.product.dto.view.ProductDetailView;
import com.bluecone.app.product.dto.view.ProductSkuView;
import com.bluecone.app.product.dto.view.ProductSpecGroupView;
import com.bluecone.app.product.dto.view.ProductSpecOptionView;
import com.bluecone.app.core.context.CurrentUserContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 商品查询相关应用服务，仅负责读场景的编排。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductQueryApplicationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CurrentUserContext currentUserContext;
    private final ProductRepository productRepository;

    /**
     * 根据商品 ID 查询商品详情（SPU + SKU + 规格 + 属性 + 小料）。
     */
    public ProductDetailView getProductDetail(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        Long tenantId = currentUserContext.getCurrentTenantId();
        Product product = productRepository.loadProductAggregate(tenantId, productId, null, null);
        if (product == null) {
            log.info("商品不存在或未启用, tenantId={}, productId={}", tenantId, productId);
            return null;
        }
        ProductDetailView view = toDetailView(product);
        log.debug("Loaded product detail, tenantId={}, productId={}", tenantId, productId);
        return view;
    }

    /**
     * 按分类列出商品（管理端读场景）。
     * 分类为空时返回当前租户下在门店配置中的可见商品。
     */
    public List<ProductDetailView> listProductsByCategory(Long categoryId) {
        Long tenantId = currentUserContext.getCurrentTenantId();
        List<Product> aggregates = productRepository.loadAvailableProductsForStore(tenantId, null, null);
        if (aggregates == null || aggregates.isEmpty()) {
            return Collections.emptyList();
        }
        return aggregates.stream()
                .filter(prod -> categoryId == null || hasCategory(prod, categoryId))
                .filter(prod -> prod.getId() != null)
                .collect(Collectors.toMap(Product::getId, this::toDetailView, (a, b) -> a))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    private boolean hasCategory(Product product, Long categoryId) {
        if (product == null || product.getCategories() == null || categoryId == null) {
            return false;
        }
        return product.getCategories().stream()
                .anyMatch(cat -> categoryId.equals(cat.getId()));
    }

    private ProductDetailView toDetailView(Product product) {
        return ProductDetailView.builder()
                .productId(product.getId())
                .tenantId(product.getTenantId())
                .productCode(product.getProductCode())
                .name(product.getName())
                .subtitle(product.getSubtitle())
                .productType(product.getProductType() != null ? product.getProductType().getCode() : null)
                .description(product.getDescription())
                .mainImage(product.getMainImage())
                .mediaGallery(product.getMediaGallery())
                .unit(product.getUnit())
                .status(product.getStatus() != null ? product.getStatus().getCode() : null)
                .sortOrder(product.getSortOrder())
                .categories(toCategoryViews(product.getCategories()))
                .skus(toSkuViews(product.getSkus()))
                .specGroups(toSpecGroups(product.getSpecGroups()))
                .attrGroups(toAttrGroups(product.getAttributeGroups()))
                .addonGroups(toAddonGroups(product.getAddonGroups()))
                .build();
    }

    private List<ProductCategoryBriefView> toCategoryViews(List<ProductCategory> categories) {
        if (categories == null) {
            return Collections.emptyList();
        }
        return categories.stream()
                .map(cat -> ProductCategoryBriefView.builder()
                        .categoryId(cat.getId())
                        .name(cat.getName())
                        .level(cat.getLevel())
                        .sortOrder(cat.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ProductSkuView> toSkuViews(List<ProductSku> skus) {
        if (skus == null) {
            return Collections.emptyList();
        }
        return skus.stream()
                .map(sku -> ProductSkuView.builder()
                        .skuId(sku.getId())
                        .skuCode(sku.getSkuCode())
                        .name(sku.getName())
                        .basePrice(sku.getBasePrice())
                        .marketPrice(sku.getMarketPrice())
                        .costPrice(sku.getCostPrice())
                        .barcode(sku.getBarcode())
                        .isDefault(sku.isDefaultSku())
                        .specCombination(toJson(sku.getSpecCombination()))
                        .status(sku.getStatus() != null ? sku.getStatus().getCode() : null)
                        .sortOrder(sku.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ProductSpecGroupView> toSpecGroups(List<ProductSpecGroup> groups) {
        if (groups == null) {
            return Collections.emptyList();
        }
        return groups.stream()
                .map(group -> ProductSpecGroupView.builder()
                        .specGroupId(group.getId())
                        .name(group.getName())
                        .selectType(group.getSelectType() != null ? group.getSelectType().getCode() : null)
                        .required(group.isRequired())
                        .maxSelect(group.getMaxSelect())
                        .sortOrder(group.getSortOrder())
                        .status(group.getStatus() != null ? group.getStatus().getCode() : null)
                        .options(toSpecOptions(group.getOptions()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<ProductSpecOptionView> toSpecOptions(List<ProductSpecOption> options) {
        if (options == null) {
            return Collections.emptyList();
        }
        return options.stream()
                .map(opt -> ProductSpecOptionView.builder()
                        .optionId(opt.getId())
                        .name(opt.getName())
                        .priceDelta(opt.getPriceDelta())
                        .isDefault(opt.isDefaultOption())
                        .sortOrder(opt.getSortOrder())
                        .status(opt.getStatus() != null ? opt.getStatus().getCode() : null)
                        .build())
                .collect(Collectors.toList());
    }

    private List<ProductAttrGroupView> toAttrGroups(List<ProductAttributeGroup> groups) {
        if (groups == null) {
            return Collections.emptyList();
        }
        return groups.stream()
                .map(group -> ProductAttrGroupView.builder()
                        .attrGroupId(group.getId())
                        .name(group.getName())
                        .scope(group.getScope())
                        .selectType(group.getSelectType() != null ? group.getSelectType().getCode() : null)
                        .required(group.isRequired())
                        .maxSelect(group.getMaxSelect())
                        .sortOrder(group.getSortOrder())
                        .status(group.getStatus() != null ? group.getStatus().getCode() : null)
                        .options(toAttrOptions(group.getOptions()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<ProductAttrOptionView> toAttrOptions(List<ProductAttributeOption> options) {
        if (options == null) {
            return Collections.emptyList();
        }
        return options.stream()
                .map(opt -> ProductAttrOptionView.builder()
                        .optionId(opt.getId())
                        .name(opt.getName())
                        .valueCode(opt.getValueCode())
                        .priceDelta(opt.getPriceDelta())
                        .sortOrder(opt.getSortOrder())
                        .status(opt.getStatus() != null ? opt.getStatus().getCode() : null)
                        .build())
                .collect(Collectors.toList());
    }

    private List<ProductAddonGroupView> toAddonGroups(List<AddonGroup> groups) {
        if (groups == null) {
            return Collections.emptyList();
        }
        return groups.stream()
                .map(group -> ProductAddonGroupView.builder()
                        .addonGroupId(group.getId())
                        .name(group.getName())
                        .type(group.getType() != null ? group.getType().getCode() : null)
                        .sortOrder(group.getSortOrder())
                        .remark(group.getRemark())
                        .required(group.getRequired())
                        .maxTotalQuantity(group.getMaxTotalQuantity() != null ? group.getMaxTotalQuantity().toPlainString() : null)
                        .items(toAddonItems(group.getItems()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<ProductAddonItemView> toAddonItems(List<AddonItem> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(item -> ProductAddonItemView.builder()
                        .addonItemId(item.getId())
                        .name(item.getName())
                        .price(item.getPrice())
                        .maxQuantity(item.getMaxQuantity())
                        .freeLimit(item.getFreeLimit())
                        .sortOrder(item.getSortOrder())
                        .status(item.getStatus() != null ? item.getStatus().getCode() : null)
                        .build())
                .collect(Collectors.toList());
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("规格组合序列化失败, value={}", value, e);
            return null;
        }
    }
}
