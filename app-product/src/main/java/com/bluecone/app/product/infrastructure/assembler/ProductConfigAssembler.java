package com.bluecone.app.product.infrastructure.assembler;

import com.bluecone.app.product.dao.entity.BcAddonGroup;
import com.bluecone.app.product.dao.entity.BcAddonItem;
import com.bluecone.app.product.dao.entity.BcProduct;
import com.bluecone.app.product.dao.entity.BcProductAddonRel;
import com.bluecone.app.product.dao.entity.BcProductAttrGroup;
import com.bluecone.app.product.dao.entity.BcProductAttrOption;
import com.bluecone.app.product.dao.entity.BcProductAttrRel;
import com.bluecone.app.product.dao.entity.BcProductCategory;
import com.bluecone.app.product.dao.entity.BcProductCategoryRel;
import com.bluecone.app.product.dao.entity.BcProductSku;
import com.bluecone.app.product.dao.entity.BcProductSpecGroup;
import com.bluecone.app.product.dao.entity.BcProductSpecOption;
import com.bluecone.app.product.dao.entity.BcProductStoreConfig;
import com.bluecone.app.product.dao.entity.BcProductTag;
import com.bluecone.app.product.dao.entity.BcProductTagRel;
import com.bluecone.app.product.domain.enums.AddonType;
import com.bluecone.app.product.domain.enums.ProductStatus;
import com.bluecone.app.product.domain.enums.ProductType;
import com.bluecone.app.product.domain.enums.SaleChannel;
import com.bluecone.app.product.domain.enums.SelectType;
import com.bluecone.app.product.domain.model.Product;
import com.bluecone.app.product.domain.model.ProductCategory;
import com.bluecone.app.product.domain.model.ProductSku;
import com.bluecone.app.product.domain.model.addon.AddonGroup;
import com.bluecone.app.product.domain.model.addon.AddonItem;
import com.bluecone.app.product.domain.model.attribute.ProductAttributeGroup;
import com.bluecone.app.product.domain.model.attribute.ProductAttributeOption;
import com.bluecone.app.product.domain.model.spec.ProductSpecGroup;
import com.bluecone.app.product.domain.model.spec.ProductSpecOption;
import com.bluecone.app.product.domain.model.store.ProductStoreConfig;
import com.bluecone.app.product.domain.model.store.TimeRange;
import com.bluecone.app.product.domain.model.tag.ProductTag;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 商品聚合装配器：将多张表的 DAO 实体组装为可缓存的领域聚合 Product。
 * <p>高隔离：领域层不感知表结构/MyBatis，统一由装配器完成实体 -> 领域模型的转换。</p>
 * <p>高并发：一次性拼装完整聚合，可直接作为缓存值使用（如放入 Redis/Caffeine）。</p>
 */
@Component
public class ProductConfigAssembler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 将各个 DAO 实体拼装为商品聚合。
     */
    public Product assembleProductAggregate(
            BcProduct product,
            List<BcProductSku> skus,
            List<BcProductSpecGroup> specGroups,
            List<BcProductSpecOption> specOptions,
            List<BcProductAttrRel> productAttrRels,
            List<BcProductAttrGroup> attrGroups,
            List<BcProductAttrOption> attrOptions,
            List<BcProductAddonRel> productAddonRels,
            List<BcAddonGroup> addonGroups,
            List<BcAddonItem> addonItems,
            List<BcProductTagRel> productTagRels,
            List<BcProductTag> tags,
            List<BcProductCategoryRel> categoryRels,
            List<BcProductCategory> categories,
            List<BcProductStoreConfig> storeConfigs
    ) {
        Product aggregate = toProduct(product);
        aggregate.setSkus(toSkuModels(skus));
        aggregate.setSpecGroups(toSpecGroups(specGroups, specOptions));
        aggregate.setAttributeGroups(toAttributeGroups(productAttrRels, attrGroups, attrOptions));
        aggregate.setAddonGroups(toAddonGroups(productAddonRels, addonGroups, addonItems));
        aggregate.setTags(toTags(productTagRels, tags));
        aggregate.setCategories(toCategories(categoryRels, categories));
        aggregate.setStoreConfigs(toStoreConfigs(storeConfigs));
        return aggregate;
    }

    /**
     * SPU 实体 -> 领域模型。
     */
    private Product toProduct(BcProduct entity) {
        if (entity == null) {
            return null;
        }
        return Product.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .productCode(entity.getProductCode())
                .name(entity.getName())
                .subtitle(entity.getSubtitle())
                .productType(ProductType.fromCode(entity.getProductType()))
                .description(entity.getDescription())
                .mainImage(entity.getMainImage())
                .mediaGallery(parseStringList(entity.getMediaGallery()))
                .unit(entity.getUnit())
                .status(ProductStatus.fromCode(entity.getStatus()))
                .sortOrder(entity.getSortOrder())
                .productMeta(parseMap(entity.getProductMeta()))
                .build();
    }

    /**
     * SKU 实体列表 -> 领域模型列表。
     */
    private List<ProductSku> toSkuModels(List<BcProductSku> skus) {
        if (skus == null || skus.isEmpty()) {
            return Collections.emptyList();
        }
        return skus.stream()
                .filter(item -> isEnabled(item.getStatus()))
                .map(item -> ProductSku.builder()
                        .id(item.getId())
                        .tenantId(item.getTenantId())
                        .productId(item.getProductId())
                        .skuCode(item.getSkuCode())
                        .name(item.getName())
                        .basePrice(item.getBasePrice())
                        .marketPrice(item.getMarketPrice())
                        .costPrice(item.getCostPrice())
                        .barcode(item.getBarcode())
                        .defaultSku(Boolean.TRUE.equals(item.getIsDefault()))
                        .specCombination(parseMap(item.getSpecCombination()))
                        .status(ProductStatus.fromCode(item.getStatus()))
                        .sortOrder(item.getSortOrder())
                        .skuMeta(parseMap(item.getSkuMeta()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 规格组/规格项 -> 领域模型列表。
     */
    private List<ProductSpecGroup> toSpecGroups(List<BcProductSpecGroup> groups, List<BcProductSpecOption> options) {
        if (groups == null || groups.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<BcProductSpecOption>> optionsByGroupId = safeList(options).stream()
                .filter(item -> isEnabled(item.getStatus()))
                .collect(Collectors.groupingBy(BcProductSpecOption::getSpecGroupId));

        return groups.stream()
                .filter(item -> isEnabled(item.getStatus()))
                .map(item -> ProductSpecGroup.builder()
                        .id(item.getId())
                        .tenantId(item.getTenantId())
                        .productId(item.getProductId())
                        .name(item.getName())
                        .selectType(SelectType.fromCode(item.getSelectType()))
                        .required(Boolean.TRUE.equals(item.getRequired()))
                        .maxSelect(item.getMaxSelect())
                        .sortOrder(item.getSortOrder())
                        .status(ProductStatus.fromCode(item.getStatus()))
                        .options(toSpecOptions(optionsByGroupId.get(item.getId())))
                        .build())
                .collect(Collectors.toList());
    }

    private List<ProductSpecOption> toSpecOptions(List<BcProductSpecOption> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        return options.stream()
                .map(item -> ProductSpecOption.builder()
                        .id(item.getId())
                        .tenantId(item.getTenantId())
                        .productId(item.getProductId())
                        .specGroupId(item.getSpecGroupId())
                        .name(item.getName())
                        .priceDelta(item.getPriceDelta())
                        .defaultOption(Boolean.TRUE.equals(item.getIsDefault()))
                        .sortOrder(item.getSortOrder())
                        .status(ProductStatus.fromCode(item.getStatus()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 属性组/属性项 -> 领域模型列表，结合商品-属性组绑定关系中的必选、选择类型、排序等配置。
     * <p>
     * 注意：此方法暂时保留旧的签名以兼容现有代码，但内部逻辑需要适配新的表结构。
     * 新的表结构中，{@code BcProductAttrRel} 是 item 级覆盖表，组级规则应使用 {@code BcProductAttrGroupRel}。
     * 由于调用方尚未更新，此处暂时将 {@code BcProductAttrRel} 当作组级绑定表使用（假设每个组只有一条 rel 记录）。
     * 后续应更新调用方，传入 {@code BcProductAttrGroupRel} 列表。
     */
    private List<ProductAttributeGroup> toAttributeGroups(List<BcProductAttrRel> rels,
                                                         List<BcProductAttrGroup> groups,
                                                         List<BcProductAttrOption> options) {
        if (rels == null || rels.isEmpty() || groups == null || groups.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, BcProductAttrRel> relByGroupId = rels.stream()
                .filter(item -> isEnabled(item.getStatus()))
                .collect(Collectors.toMap(BcProductAttrRel::getAttrGroupId, Function.identity(), (a, b) -> a));

        Map<Long, List<BcProductAttrOption>> optionsByGroupId = safeList(options).stream()
                .filter(item -> isEnabled(item.getStatus()))
                .collect(Collectors.groupingBy(BcProductAttrOption::getAttrGroupId));

        List<ProductAttributeGroup> result = new ArrayList<>();
        for (BcProductAttrGroup group : groups) {
            BcProductAttrRel rel = relByGroupId.get(group.getId());
            if (rel == null) {
                continue;
            }
            boolean enabled = isEnabled(group.getStatus()) && isEnabled(rel.getStatus());
            ProductStatus status = enabled ? ProductStatus.ENABLED : ProductStatus.DISABLED;
            // 新的表结构中，BcProductAttrRel 不再有 selectType 字段，直接使用素材库默认值
            SelectType selectType = SelectType.fromCode(group.getSelectType());
            // 新的表结构中，BcProductAttrRel 不再有 required 字段，直接使用素材库默认值
            Boolean required = group.getRequired();
            Integer sortOrder = rel.getSortOrder() != null ? rel.getSortOrder() : group.getSortOrder();
            ProductAttributeGroup model = ProductAttributeGroup.builder()
                    .id(group.getId())
                    .tenantId(group.getTenantId())
                    .name(group.getName())
                    .scope(group.getScope())
                    .selectType(selectType)
                    .required(Boolean.TRUE.equals(required))
                    .maxSelect(group.getMaxSelect())
                    .sortOrder(sortOrder)
                    .remark(group.getRemark())
                    .status(status)
                    .options(toAttributeOptions(optionsByGroupId.get(group.getId())))
                    .build();
            result.add(model);
        }
        return result;
    }

    private List<ProductAttributeOption> toAttributeOptions(List<BcProductAttrOption> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        return options.stream()
                .map(item -> ProductAttributeOption.builder()
                        .id(item.getId())
                        .tenantId(item.getTenantId())
                        .attrGroupId(item.getAttrGroupId())
                        .name(item.getName())
                        .valueCode(item.getValueCode())
                        .priceDelta(item.getPriceDelta())
                        .sortOrder(item.getSortOrder())
                        .status(ProductStatus.fromCode(item.getStatus()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 小料组/小料项 -> 领域模型列表，结合商品-小料组绑定关系中的必选和数量上限。
     * <p>
     * 注意：此方法暂时保留旧的签名以兼容现有代码，但内部逻辑需要适配新的表结构。
     * 新的表结构中，{@code BcProductAddonRel} 是 item 级覆盖表，组级规则应使用 {@code BcProductAddonGroupRel}。
     * 由于调用方尚未更新，此处暂时将 {@code BcProductAddonRel} 当作组级绑定表使用（假设每个组只有一条 rel 记录）。
     * 后续应更新调用方，传入 {@code BcProductAddonGroupRel} 列表。
     */
    private List<AddonGroup> toAddonGroups(List<BcProductAddonRel> rels,
                                           List<BcAddonGroup> groups,
                                           List<BcAddonItem> items) {
        if (rels == null || rels.isEmpty() || groups == null || groups.isEmpty()) {
            return Collections.emptyList();
        }
        // 暂时将 BcProductAddonRel 按 addonGroupId 分组，假设每个组只有一条记录
        Map<Long, BcProductAddonRel> relByGroupId = rels.stream()
                .filter(item -> isEnabled(item.getStatus()))
                .collect(Collectors.toMap(BcProductAddonRel::getAddonGroupId, Function.identity(), (a, b) -> a));
        Map<Long, List<BcAddonItem>> itemsByGroupId = safeList(items).stream()
                .filter(item -> isEnabled(item.getStatus()))
                .collect(Collectors.groupingBy(BcAddonItem::getGroupId));

        List<AddonGroup> result = new ArrayList<>();
        for (BcAddonGroup group : groups) {
            BcProductAddonRel rel = relByGroupId.get(group.getId());
            if (rel == null) {
                continue;
            }
            boolean enabled = isEnabled(group.getStatus()) && isEnabled(rel.getStatus());
            ProductStatus status = enabled ? ProductStatus.ENABLED : ProductStatus.DISABLED;
            Integer sortOrder = rel.getSortOrder() != null ? rel.getSortOrder() : group.getSortOrder();
            AddonGroup model = AddonGroup.builder()
                    .id(group.getId())
                    .tenantId(group.getTenantId())
                    .name(group.getName())
                    .type(AddonType.fromCode(group.getType()))
                    .status(status)
                    .sortOrder(sortOrder)
                    .remark(group.getRemark())
                    // 新的表结构中，BcProductAddonRel 不再有 required 和 maxTotalQuantity 字段
                    // 暂时设置为默认值，后续应使用 BcProductAddonGroupRel
                    .required(false)
                    .maxTotalQuantity(null)
                    .items(toAddonItems(itemsByGroupId.get(group.getId())))
                    .build();
            result.add(model);
        }
        return result;
    }

    private List<AddonItem> toAddonItems(List<BcAddonItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(item -> AddonItem.builder()
                        .id(item.getId())
                        .tenantId(item.getTenantId())
                        .groupId(item.getGroupId())
                        .name(item.getName())
                        .price(item.getPrice())
                        .maxQuantity(item.getMaxQuantity())
                        .freeLimit(item.getFreeLimit())
                        .sortOrder(item.getSortOrder())
                        .status(ProductStatus.fromCode(item.getStatus()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 标签 + 绑定关系 -> 领域模型列表。
     */
    private List<ProductTag> toTags(List<BcProductTagRel> rels, List<BcProductTag> tags) {
        if (rels == null || rels.isEmpty() || tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, BcProductTagRel> relByTagId = rels.stream()
                .filter(item -> isEnabled(item.getStatus()))
                .collect(Collectors.toMap(BcProductTagRel::getTagId, Function.identity(), (a, b) -> a));
        List<ProductTag> result = new ArrayList<>();
        for (BcProductTag tag : tags) {
            BcProductTagRel rel = relByTagId.get(tag.getId());
            if (rel == null || !isEnabled(tag.getStatus())) {
                continue;
            }
            ProductTag model = ProductTag.builder()
                    .id(tag.getId())
                    .tenantId(tag.getTenantId())
                    .name(tag.getName())
                    .style(parseMap(tag.getStyle()))
                    .status(ProductStatus.fromCode(tag.getStatus()))
                    .sortOrder(rel.getSortOrder())
                    .build();
            result.add(model);
        }
        return result;
    }

    /**
     * 分类 + 绑定关系 -> 领域模型列表。
     */
    private List<ProductCategory> toCategories(List<BcProductCategoryRel> rels, List<BcProductCategory> categories) {
        if (rels == null || rels.isEmpty() || categories == null || categories.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, BcProductCategoryRel> relByCategoryId = rels.stream()
                .filter(item -> isEnabled(item.getStatus()))
                .collect(Collectors.toMap(BcProductCategoryRel::getCategoryId, Function.identity(), (a, b) -> a));
        List<ProductCategory> result = new ArrayList<>();
        for (BcProductCategory category : categories) {
            BcProductCategoryRel rel = relByCategoryId.get(category.getId());
            if (rel == null || !isEnabled(category.getStatus())) {
                continue;
            }
            ProductCategory model = ProductCategory.builder()
                    .id(category.getId())
                    .tenantId(category.getTenantId())
                    .parentId(category.getParentId())
                    .name(category.getName())
                    .type(category.getType())
                    .level(category.getLevel())
                    .icon(category.getIcon())
                    .status(ProductStatus.fromCode(category.getStatus()))
                    .sortOrder(rel.getSortOrder() != null ? rel.getSortOrder() : category.getSortOrder())
                    .remark(category.getRemark())
                    .build();
            result.add(model);
        }
        return result;
    }

    /**
     * 门店维度配置实体 -> 领域模型列表。
     */
    private List<ProductStoreConfig> toStoreConfigs(List<BcProductStoreConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            return Collections.emptyList();
        }
        return configs.stream()
                .filter(item -> isEnabled(item.getStatus()))
                .map(item -> ProductStoreConfig.builder()
                        .id(item.getId())
                        .tenantId(item.getTenantId())
                        .storeId(item.getStoreId())
                        .productId(item.getProductId())
                        .skuId(item.getSkuId())
                        .channel(SaleChannel.fromCode(item.getChannel()))
                        .visible(Boolean.TRUE.equals(item.getVisible()))
                        .overridePrice(item.getOverridePrice())
                        .availableOrderTypes(parseStringList(item.getAvailableOrderTypes()))
                        .availableTimeRanges(parseTimeRanges(item.getAvailableTimeRanges()))
                        .dailySoldOutLimit(item.getDailySoldOutLimit())
                        .sortOrder(item.getSortOrder())
                        .status(ProductStatus.fromCode(item.getStatus()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private List<TimeRange> parseTimeRanges(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, String>> raw = OBJECT_MAPPER.readValue(json, new TypeReference<List<Map<String, String>>>() {
            });
            return raw.stream()
                    .map(item -> new TimeRange(item.get("from"), item.get("to")))
                    .filter(tr -> Objects.nonNull(tr.getFromTime()) && Objects.nonNull(tr.getToTime()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private boolean isEnabled(Integer status) {
        return status != null && status == 1;
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
