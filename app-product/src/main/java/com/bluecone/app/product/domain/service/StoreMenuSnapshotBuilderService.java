package com.bluecone.app.product.domain.service;

import com.bluecone.app.product.domain.model.Product;
import com.bluecone.app.product.domain.model.ProductCategory;
import com.bluecone.app.product.domain.model.ProductSku;
import com.bluecone.app.product.domain.model.addon.AddonGroup;
import com.bluecone.app.product.domain.model.addon.AddonItem;
import com.bluecone.app.product.domain.model.attribute.ProductAttributeGroup;
import com.bluecone.app.product.domain.model.attribute.ProductAttributeOption;
import com.bluecone.app.product.domain.model.menu.StoreMenuAddonGroupView;
import com.bluecone.app.product.domain.model.menu.StoreMenuAddonItemView;
import com.bluecone.app.product.domain.model.menu.StoreMenuAttrGroupView;
import com.bluecone.app.product.domain.model.menu.StoreMenuAttrOptionView;
import com.bluecone.app.product.domain.model.menu.StoreMenuCategoryView;
import com.bluecone.app.product.domain.model.menu.StoreMenuProductView;
import com.bluecone.app.product.domain.model.menu.StoreMenuSkuView;
import com.bluecone.app.product.domain.model.menu.StoreMenuSnapshotModel;
import com.bluecone.app.product.domain.model.menu.StoreMenuSpecGroupView;
import com.bluecone.app.product.domain.model.menu.StoreMenuSpecOptionView;
import com.bluecone.app.product.domain.model.spec.ProductSpecGroup;
import com.bluecone.app.product.domain.model.spec.ProductSpecOption;
import com.bluecone.app.product.domain.repository.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 菜单快照构建服务：聚合多张表的数据，拼装可直接给前端/网关使用的菜单快照模型。
 * <p>高并发读：构建完成的模型会序列化为 JSON 存入快照表，读路径无需多表 join。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoreMenuSnapshotBuilderService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ProductRepository productRepository;

    /**
     * 构建指定门店/渠道/场景下的菜单快照模型。
     */
    public StoreMenuSnapshotModel buildStoreMenuSnapshot(Long tenantId, Long storeId, String channel, String orderScene) {
        List<Product> products = productRepository.loadAvailableProductsForStore(tenantId, storeId, channel);
        // 去重，防止多渠道/多配置导致重复聚合
        Map<Long, Product> productMap = products == null ? Collections.emptyMap()
                : products.stream()
                .filter(p -> p.getId() != null)
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        products = new ArrayList<>(productMap.values());
        if (products == null || products.isEmpty()) {
            return StoreMenuSnapshotModel.builder()
                    .tenantId(tenantId)
                    .storeId(storeId)
                    .channel(channel)
                    .orderScene(orderScene)
                    .categories(Collections.emptyList())
                    .build();
        }
        Map<Long, StoreMenuCategoryView> categoryMap = new HashMap<>();
        for (Product product : products) {
            List<ProductCategory> categories = product.getCategories();
            if (categories == null || categories.isEmpty()) {
                // 若无分类，放入一个虚拟分类（可按需要扩展）
                categories = Collections.singletonList(ProductCategory.builder()
                        .id(-1L).name("未分组").sortOrder(0).build());
            }
            for (ProductCategory category : categories) {
                StoreMenuCategoryView categoryView = categoryMap.computeIfAbsent(
                        category.getId(),
                        id -> StoreMenuCategoryView.builder()
                                .categoryId(id)
                                .name(category.getName())
                                .sortOrder(category.getSortOrder())
                                .products(new ArrayList<>())
                                .build()
                );
                categoryView.getProducts().add(toProductView(product));
            }
        }
        List<StoreMenuCategoryView> sortedCategories = categoryMap.values().stream()
                .peek(cat -> cat.getProducts().sort(Comparator.comparing(StoreMenuProductView::getProductId)))
                .sorted(Comparator.comparing(StoreMenuCategoryView::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(StoreMenuCategoryView::getCategoryId))
                .collect(Collectors.toList());
        return StoreMenuSnapshotModel.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .channel(channel)
                .orderScene(orderScene)
                .categories(sortedCategories)
                .build();
    }

    /**
     * 将快照模型序列化为 JSON 字符串，供快照表持久化。
     */
    public String buildMenuJson(StoreMenuSnapshotModel model) {
        if (model == null) {
            return "{}";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(model);
        } catch (JsonProcessingException e) {
            log.error("序列化菜单快照失败 model={}", model, e);
            return "{}";
        }
    }

    private StoreMenuProductView toProductView(Product product) {
        return StoreMenuProductView.builder()
                .productId(product.getId())
                .name(product.getName())
                .subtitle(product.getSubtitle())
                .mainImage(product.getMainImage())
                .tags(product.getTags() == null ? Collections.emptyList()
                        : product.getTags().stream().map(tag -> tag.getName()).collect(Collectors.toList()))
                .productMeta(product.getProductMeta())
                .skus(toSkuViews(product.getSkus()))
                .specGroups(toSpecGroups(product.getSpecGroups()))
                .attrGroups(toAttrGroups(product.getAttributeGroups()))
                .addonGroups(toAddonGroups(product.getAddonGroups()))
                .build();
    }

    private List<StoreMenuSkuView> toSkuViews(List<ProductSku> skus) {
        if (skus == null) {
            return Collections.emptyList();
        }
        return skus.stream()
                .sorted(Comparator.comparing(ProductSku::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(sku -> StoreMenuSkuView.builder()
                        .skuId(sku.getId())
                        .name(sku.getName())
                        .price(sku.getBasePrice())
                        .originPrice(defaultIfNull(sku.getMarketPrice(), sku.getBasePrice()))
                        .defaultSku(sku.isDefaultSku())
                        .ext(sku.getSkuMeta())
                        .build())
                .collect(Collectors.toList());
    }

    private List<StoreMenuSpecGroupView> toSpecGroups(List<ProductSpecGroup> groups) {
        if (groups == null) {
            return Collections.emptyList();
        }
        return groups.stream()
                .sorted(Comparator.comparing(ProductSpecGroup::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(group -> StoreMenuSpecGroupView.builder()
                        .specGroupId(group.getId())
                        .name(group.getName())
                        .selectType(group.getSelectType() == null ? null : group.getSelectType().getCode())
                        .required(group.isRequired())
                        .maxSelect(group.getMaxSelect())
                        .sortOrder(group.getSortOrder())
                        .options(toSpecOptions(group.getOptions()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<StoreMenuSpecOptionView> toSpecOptions(List<ProductSpecOption> options) {
        if (options == null) {
            return Collections.emptyList();
        }
        return options.stream()
                .sorted(Comparator.comparing(ProductSpecOption::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(opt -> StoreMenuSpecOptionView.builder()
                        .optionId(opt.getId())
                        .name(opt.getName())
                        .priceDelta(opt.getPriceDelta())
                        .defaultOption(opt.isDefaultOption())
                        .sortOrder(opt.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

    private List<StoreMenuAttrGroupView> toAttrGroups(List<ProductAttributeGroup> groups) {
        if (groups == null) {
            return Collections.emptyList();
        }
        return groups.stream()
                .sorted(Comparator.comparing(ProductAttributeGroup::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(group -> StoreMenuAttrGroupView.builder()
                        .attrGroupId(group.getId())
                        .name(group.getName())
                        .scope(group.getScope())
                        .selectType(group.getSelectType() == null ? null : group.getSelectType().getCode())
                        .required(group.isRequired())
                        .maxSelect(group.getMaxSelect())
                        .sortOrder(group.getSortOrder())
                        .options(toAttrOptions(group.getOptions()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<StoreMenuAttrOptionView> toAttrOptions(List<ProductAttributeOption> options) {
        if (options == null) {
            return Collections.emptyList();
        }
        return options.stream()
                .sorted(Comparator.comparing(ProductAttributeOption::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(opt -> StoreMenuAttrOptionView.builder()
                        .optionId(opt.getId())
                        .name(opt.getName())
                        .valueCode(opt.getValueCode())
                        .priceDelta(opt.getPriceDelta())
                        .sortOrder(opt.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

    private List<StoreMenuAddonGroupView> toAddonGroups(List<AddonGroup> groups) {
        if (groups == null) {
            return Collections.emptyList();
        }
        return groups.stream()
                .sorted(Comparator.comparing(AddonGroup::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(group -> StoreMenuAddonGroupView.builder()
                        .addonGroupId(group.getId())
                        .name(group.getName())
                        .type(group.getType() == null ? null : group.getType().getCode())
                        .sortOrder(group.getSortOrder())
                        .remark(group.getRemark())
                        .required(group.getRequired())
                        .maxTotalQuantity(group.getMaxTotalQuantity() != null ? group.getMaxTotalQuantity().toPlainString() : null)
                        .items(toAddonItems(group.getItems()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<StoreMenuAddonItemView> toAddonItems(List<AddonItem> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        return items.stream()
                .sorted(Comparator.comparing(AddonItem::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(item -> StoreMenuAddonItemView.builder()
                        .addonItemId(item.getId())
                        .name(item.getName())
                        .price(item.getPrice())
                        .maxQuantity(item.getMaxQuantity())
                        .freeLimit(item.getFreeLimit())
                        .sortOrder(item.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

    private BigDecimal defaultIfNull(BigDecimal value, BigDecimal fallback) {
        return value != null ? value : fallback;
    }
}
