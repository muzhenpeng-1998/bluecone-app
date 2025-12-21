package com.bluecone.app.product.domain.service;

import com.bluecone.app.product.dao.entity.*;
import com.bluecone.app.product.domain.model.menu.StoreMenuCategoryView;
import com.bluecone.app.product.domain.model.menu.StoreMenuProductView;
import com.bluecone.app.product.domain.model.menu.StoreMenuSkuView;
import com.bluecone.app.product.domain.model.menu.StoreMenuSnapshotModel;
import com.bluecone.app.product.dto.view.unified.OptionGroupView;
import com.bluecone.app.product.infrastructure.assembler.UnifiedOptionGroupAssembler;
import com.bluecone.app.product.dao.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * 菜单快照构建服务：聚合多张表的数据，拼装可直接给前端/网关使用的菜单快照模型。
 * <p>
 * Prompt 07 重构要点：
 * <ul>
 *   <li>使用 {@link UnifiedOptionGroupAssembler} 构建统一的 {@link OptionGroupView}</li>
 *   <li>构建时执行过滤规则：category/product/optionGroup/item 的 enabled 和定时展示窗口</li>
 *   <li>支持 {@code now} 参数，用于定时展示判断</li>
 *   <li>输出结构稳定，包含 categories/products/optionGroups/skus</li>
 * </ul>
 * <p>高并发读：构建完成的模型会序列化为 JSON 存入快照表，读路径无需多表 join。</p>
 *
 * @author System
 * @since 2025-12-21
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoreMenuSnapshotBuilderService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final UnifiedOptionGroupAssembler unifiedOptionGroupAssembler;
    
    // Mappers for direct DB access
    private final BcProductStoreConfigMapper productStoreConfigMapper;
    private final BcProductMapper productMapper;
    private final BcProductSkuMapper productSkuMapper;
    private final BcProductCategoryRelMapper productCategoryRelMapper;
    private final BcProductCategoryMapper productCategoryMapper;
    private final BcProductSpecGroupMapper productSpecGroupMapper;
    private final BcProductSpecOptionMapper productSpecOptionMapper;
    private final BcProductAttrGroupRelMapper productAttrGroupRelMapper;
    private final BcProductAttrGroupMapper productAttrGroupMapper;
    private final BcProductAttrOptionMapper productAttrOptionMapper;
    private final BcProductAttrRelMapper productAttrRelMapper;
    private final BcProductAddonGroupRelMapper productAddonGroupRelMapper;
    private final BcAddonGroupMapper addonGroupMapper;
    private final BcAddonItemMapper addonItemMapper;
    private final BcProductAddonRelMapper productAddonRelMapper;

    /**
     * 构建指定门店/渠道/场景下的菜单快照模型。
     * <p>
     * Prompt 07: 添加 {@code now} 参数，用于定时展示过滤。
     *
     * @param tenantId   租户ID
     * @param storeId    门店ID
     * @param channel    渠道（ALL, DINE_IN, TAKEAWAY, DELIVERY, PICKUP）
     * @param orderScene 订单场景（DEFAULT, BREAKFAST, LUNCH, DINNER, NIGHT）
     * @param now        当前时间，用于定时展示判断（为 null 时使用 LocalDateTime.now()）
     * @return 菜单快照模型
     */
    public StoreMenuSnapshotModel buildStoreMenuSnapshot(
            Long tenantId, 
            Long storeId, 
            String channel, 
            String orderScene,
            LocalDateTime now
    ) {
        if (now == null) {
            now = LocalDateTime.now();
        }
        
        log.info("开始构建菜单快照: tenantId={}, storeId={}, channel={}, orderScene={}, now={}", 
                tenantId, storeId, channel, orderScene, now);

        // 1. 查询门店可售商品配置
        List<BcProductStoreConfig> storeConfigs = loadStoreConfigs(tenantId, storeId, channel);
        if (CollectionUtils.isEmpty(storeConfigs)) {
            log.info("门店无可售商品配置: tenantId={}, storeId={}, channel={}", tenantId, storeId, channel);
            return buildEmptySnapshot(tenantId, storeId, channel, orderScene);
        }

        Set<Long> productIds = storeConfigs.stream()
                .map(BcProductStoreConfig::getProductId)
                .collect(Collectors.toSet());

        // 2. 批量加载商品及其关联数据
        Map<Long, BcProduct> productMap = loadProducts(tenantId, productIds, now);
        if (productMap.isEmpty()) {
            log.info("无启用的商品: tenantId={}, storeId={}, productIds={}", tenantId, storeId, productIds);
            return buildEmptySnapshot(tenantId, storeId, channel, orderScene);
        }

        Map<Long, List<BcProductSku>> skuMap = loadSkus(tenantId, productMap.keySet(), now);
        Map<Long, List<BcProductCategoryRel>> categoryRelMap = loadCategoryRels(tenantId, productMap.keySet());
        Map<Long, BcProductCategory> categoryMap = loadCategories(tenantId, categoryRelMap, now);
        
        // 规格组/选项
        Map<Long, List<BcProductSpecGroup>> specGroupMap = loadSpecGroups(tenantId, productMap.keySet());
        Map<Long, List<BcProductSpecOption>> specOptionMap = loadSpecOptions(tenantId, specGroupMap);
        
        // 属性组/选项
        Map<Long, List<BcProductAttrGroupRel>> attrGroupRelMap = loadAttrGroupRels(tenantId, productMap.keySet());
        Map<Long, BcProductAttrGroup> attrGroupMap = loadAttrGroups(tenantId, attrGroupRelMap);
        Map<Long, List<BcProductAttrOption>> attrOptionMap = loadAttrOptions(tenantId, attrGroupMap.keySet());
        Map<Long, List<BcProductAttrRel>> attrRelMap = loadAttrRels(tenantId, productMap.keySet());
        
        // 小料组/小料项
        Map<Long, List<BcProductAddonGroupRel>> addonGroupRelMap = loadAddonGroupRels(tenantId, productMap.keySet());
        Map<Long, BcAddonGroup> addonGroupMap = loadAddonGroups(tenantId, addonGroupRelMap);
        Map<Long, List<BcAddonItem>> addonItemMap = loadAddonItems(tenantId, addonGroupMap.keySet());
        Map<Long, List<BcProductAddonRel>> addonRelMap = loadAddonRels(tenantId, productMap.keySet());

        // 3. 构建分类视图
        Map<Long, StoreMenuCategoryView> categoryViewMap = new HashMap<>();
        
        for (BcProduct product : productMap.values()) {
            // 过滤：商品必须启用且在展示窗口内
            if (!isEnabled(product.getStatus()) || !isInDisplayWindow(product.getDisplayStartAt(), product.getDisplayEndAt(), now)) {
                continue;
            }
            
            // 构建商品视图
            StoreMenuProductView productView = buildProductView(
                    product,
                    skuMap.get(product.getId()),
                    specGroupMap.get(product.getId()),
                    specOptionMap,
                    attrGroupRelMap.get(product.getId()),
                    attrGroupMap,
                    attrOptionMap,
                    attrRelMap.get(product.getId()),
                    addonGroupRelMap.get(product.getId()),
                    addonGroupMap,
                    addonItemMap,
                    addonRelMap.get(product.getId()),
                    now
            );
            
            // 如果商品没有有效的 SKU，跳过
            if (CollectionUtils.isEmpty(productView.getSkus())) {
                log.warn("商品无有效SKU，跳过: productId={}", product.getId());
                continue;
            }
            
            // 获取商品的分类
            List<BcProductCategoryRel> categoryRels = categoryRelMap.get(product.getId());
            if (CollectionUtils.isEmpty(categoryRels)) {
                // 无分类，放入默认分类
                StoreMenuCategoryView defaultCategory = categoryViewMap.computeIfAbsent(
                        -1L,
                        id -> StoreMenuCategoryView.builder()
                                .categoryId(-1L)
                                .name("未分组")
                                .sortOrder(0)
                                .products(new ArrayList<>())
                                .build()
                );
                defaultCategory.getProducts().add(productView);
            } else {
                for (BcProductCategoryRel rel : categoryRels) {
                    BcProductCategory category = categoryMap.get(rel.getCategoryId());
                    if (category == null) {
                        continue; // 分类不存在或已过滤
                    }
                    
                    StoreMenuCategoryView categoryView = categoryViewMap.computeIfAbsent(
                            category.getId(),
                            id -> StoreMenuCategoryView.builder()
                                    .categoryId(id)
                                    .name(category.getName())
                                    .sortOrder(category.getSortOrder())
                                    .products(new ArrayList<>())
                                    .build()
                    );
                    categoryView.getProducts().add(productView);
                }
            }
        }

        // 4. 排序并返回
        List<StoreMenuCategoryView> sortedCategories = categoryViewMap.values().stream()
                .peek(cat -> cat.getProducts().sort(
                        Comparator.comparing(StoreMenuProductView::getProductId)))
                .sorted(Comparator.comparing(StoreMenuCategoryView::getSortOrder, Comparator.reverseOrder())
                        .thenComparing(StoreMenuCategoryView::getCategoryId))
                .collect(Collectors.toList());

        log.info("菜单快照构建完成: tenantId={}, storeId={}, categories={}, products={}", 
                tenantId, storeId, sortedCategories.size(), 
                sortedCategories.stream().mapToInt(c -> c.getProducts().size()).sum());

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

    // ===== 私有辅助方法 =====

    private StoreMenuSnapshotModel buildEmptySnapshot(Long tenantId, Long storeId, String channel, String orderScene) {
        return StoreMenuSnapshotModel.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .channel(channel)
                .orderScene(orderScene)
                .categories(Collections.emptyList())
                .build();
    }

    private List<BcProductStoreConfig> loadStoreConfigs(Long tenantId, Long storeId, String channel) {
        String channelCode = channel == null ? null : channel.toUpperCase();
        LambdaQueryWrapper<BcProductStoreConfig> wrapper = new LambdaQueryWrapper<BcProductStoreConfig>()
                .eq(BcProductStoreConfig::getTenantId, tenantId)
                .eq(BcProductStoreConfig::getStoreId, storeId)
                .eq(BcProductStoreConfig::getVisible, true)
                .eq(BcProductStoreConfig::getStatus, 1);
        if (channelCode != null) {
            wrapper.in(BcProductStoreConfig::getChannel, List.of("ALL", channelCode));
        }
        return productStoreConfigMapper.selectList(wrapper);
    }

    private Map<Long, BcProduct> loadProducts(Long tenantId, Set<Long> productIds, LocalDateTime now) {
        if (CollectionUtils.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        List<BcProduct> products = productMapper.selectList(new LambdaQueryWrapper<BcProduct>()
                .eq(BcProduct::getTenantId, tenantId)
                .in(BcProduct::getId, productIds)
                .eq(BcProduct::getStatus, 1));
        
        // 过滤：商品必须在展示窗口内
        return products.stream()
                .filter(p -> isInDisplayWindow(p.getDisplayStartAt(), p.getDisplayEndAt(), now))
                .collect(Collectors.toMap(BcProduct::getId, p -> p));
    }

    private Map<Long, List<BcProductSku>> loadSkus(Long tenantId, Set<Long> productIds, LocalDateTime now) {
        if (CollectionUtils.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        List<BcProductSku> skus = productSkuMapper.selectList(new LambdaQueryWrapper<BcProductSku>()
                .eq(BcProductSku::getTenantId, tenantId)
                .in(BcProductSku::getProductId, productIds)
                .eq(BcProductSku::getStatus, 1));
        
        // 过滤：SKU 必须在展示窗口内
        return skus.stream()
                .filter(sku -> isInDisplayWindow(sku.getDisplayStartAt(), sku.getDisplayEndAt(), now))
                .collect(Collectors.groupingBy(BcProductSku::getProductId));
    }

    private Map<Long, List<BcProductCategoryRel>> loadCategoryRels(Long tenantId, Set<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        List<BcProductCategoryRel> rels = productCategoryRelMapper.selectList(new LambdaQueryWrapper<BcProductCategoryRel>()
                .eq(BcProductCategoryRel::getTenantId, tenantId)
                .in(BcProductCategoryRel::getProductId, productIds)
                .eq(BcProductCategoryRel::getStatus, 1));
        return rels.stream().collect(Collectors.groupingBy(BcProductCategoryRel::getProductId));
    }

    private Map<Long, BcProductCategory> loadCategories(Long tenantId, Map<Long, List<BcProductCategoryRel>> categoryRelMap, LocalDateTime now) {
        Set<Long> categoryIds = categoryRelMap.values().stream()
                .flatMap(List::stream)
                .map(BcProductCategoryRel::getCategoryId)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(categoryIds)) {
            return Collections.emptyMap();
        }
        List<BcProductCategory> categories = productCategoryMapper.selectList(new LambdaQueryWrapper<BcProductCategory>()
                .eq(BcProductCategory::getTenantId, tenantId)
                .in(BcProductCategory::getId, categoryIds)
                .eq(BcProductCategory::getStatus, 1));
        
        // 过滤：分类必须在展示窗口内
        return categories.stream()
                .filter(cat -> isInDisplayWindow(cat.getDisplayStartAt(), cat.getDisplayEndAt(), now))
                .collect(Collectors.toMap(BcProductCategory::getId, cat -> cat));
    }

    private Map<Long, List<BcProductSpecGroup>> loadSpecGroups(Long tenantId, Set<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        List<BcProductSpecGroup> groups = productSpecGroupMapper.selectList(new LambdaQueryWrapper<BcProductSpecGroup>()
                .eq(BcProductSpecGroup::getTenantId, tenantId)
                .in(BcProductSpecGroup::getProductId, productIds)
                .eq(BcProductSpecGroup::getStatus, 1));
        return groups.stream().collect(Collectors.groupingBy(BcProductSpecGroup::getProductId));
    }

    private Map<Long, List<BcProductSpecOption>> loadSpecOptions(Long tenantId, Map<Long, List<BcProductSpecGroup>> specGroupMap) {
        Set<Long> specGroupIds = specGroupMap.values().stream()
                .flatMap(List::stream)
                .map(BcProductSpecGroup::getId)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(specGroupIds)) {
            return Collections.emptyMap();
        }
        List<BcProductSpecOption> options = productSpecOptionMapper.selectList(new LambdaQueryWrapper<BcProductSpecOption>()
                .eq(BcProductSpecOption::getTenantId, tenantId)
                .in(BcProductSpecOption::getSpecGroupId, specGroupIds)
                .eq(BcProductSpecOption::getStatus, 1));
        return options.stream().collect(Collectors.groupingBy(BcProductSpecOption::getSpecGroupId));
    }

    private Map<Long, List<BcProductAttrGroupRel>> loadAttrGroupRels(Long tenantId, Set<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        List<BcProductAttrGroupRel> rels = productAttrGroupRelMapper.selectList(new LambdaQueryWrapper<BcProductAttrGroupRel>()
                .eq(BcProductAttrGroupRel::getTenantId, tenantId)
                .in(BcProductAttrGroupRel::getProductId, productIds)
                .eq(BcProductAttrGroupRel::getStatus, 1));
        return rels.stream().collect(Collectors.groupingBy(BcProductAttrGroupRel::getProductId));
    }

    private Map<Long, BcProductAttrGroup> loadAttrGroups(Long tenantId, Map<Long, List<BcProductAttrGroupRel>> attrGroupRelMap) {
        Set<Long> attrGroupIds = attrGroupRelMap.values().stream()
                .flatMap(List::stream)
                .map(BcProductAttrGroupRel::getAttrGroupId)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(attrGroupIds)) {
            return Collections.emptyMap();
        }
        List<BcProductAttrGroup> groups = productAttrGroupMapper.selectList(new LambdaQueryWrapper<BcProductAttrGroup>()
                .eq(BcProductAttrGroup::getTenantId, tenantId)
                .in(BcProductAttrGroup::getId, attrGroupIds)
                .eq(BcProductAttrGroup::getStatus, 1));
        return groups.stream().collect(Collectors.toMap(BcProductAttrGroup::getId, g -> g));
    }

    private Map<Long, List<BcProductAttrOption>> loadAttrOptions(Long tenantId, Set<Long> attrGroupIds) {
        if (CollectionUtils.isEmpty(attrGroupIds)) {
            return Collections.emptyMap();
        }
        List<BcProductAttrOption> options = productAttrOptionMapper.selectList(new LambdaQueryWrapper<BcProductAttrOption>()
                .eq(BcProductAttrOption::getTenantId, tenantId)
                .in(BcProductAttrOption::getAttrGroupId, attrGroupIds)
                .eq(BcProductAttrOption::getStatus, 1));
        return options.stream().collect(Collectors.groupingBy(BcProductAttrOption::getAttrGroupId));
    }

    private Map<Long, List<BcProductAttrRel>> loadAttrRels(Long tenantId, Set<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        List<BcProductAttrRel> rels = productAttrRelMapper.selectList(new LambdaQueryWrapper<BcProductAttrRel>()
                .eq(BcProductAttrRel::getTenantId, tenantId)
                .in(BcProductAttrRel::getProductId, productIds)
                .eq(BcProductAttrRel::getStatus, 1));
        return rels.stream().collect(Collectors.groupingBy(BcProductAttrRel::getProductId));
    }

    private Map<Long, List<BcProductAddonGroupRel>> loadAddonGroupRels(Long tenantId, Set<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        List<BcProductAddonGroupRel> rels = productAddonGroupRelMapper.selectList(new LambdaQueryWrapper<BcProductAddonGroupRel>()
                .eq(BcProductAddonGroupRel::getTenantId, tenantId)
                .in(BcProductAddonGroupRel::getProductId, productIds)
                .eq(BcProductAddonGroupRel::getStatus, 1));
        return rels.stream().collect(Collectors.groupingBy(BcProductAddonGroupRel::getProductId));
    }

    private Map<Long, BcAddonGroup> loadAddonGroups(Long tenantId, Map<Long, List<BcProductAddonGroupRel>> addonGroupRelMap) {
        Set<Long> addonGroupIds = addonGroupRelMap.values().stream()
                .flatMap(List::stream)
                .map(BcProductAddonGroupRel::getAddonGroupId)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(addonGroupIds)) {
            return Collections.emptyMap();
        }
        List<BcAddonGroup> groups = addonGroupMapper.selectList(new LambdaQueryWrapper<BcAddonGroup>()
                .eq(BcAddonGroup::getTenantId, tenantId)
                .in(BcAddonGroup::getId, addonGroupIds)
                .eq(BcAddonGroup::getStatus, 1));
        return groups.stream().collect(Collectors.toMap(BcAddonGroup::getId, g -> g));
    }

    private Map<Long, List<BcAddonItem>> loadAddonItems(Long tenantId, Set<Long> addonGroupIds) {
        if (CollectionUtils.isEmpty(addonGroupIds)) {
            return Collections.emptyMap();
        }
        List<BcAddonItem> items = addonItemMapper.selectList(new LambdaQueryWrapper<BcAddonItem>()
                .eq(BcAddonItem::getTenantId, tenantId)
                .in(BcAddonItem::getGroupId, addonGroupIds)
                .eq(BcAddonItem::getStatus, 1));
        return items.stream().collect(Collectors.groupingBy(BcAddonItem::getGroupId));
    }

    private Map<Long, List<BcProductAddonRel>> loadAddonRels(Long tenantId, Set<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        List<BcProductAddonRel> rels = productAddonRelMapper.selectList(new LambdaQueryWrapper<BcProductAddonRel>()
                .eq(BcProductAddonRel::getTenantId, tenantId)
                .in(BcProductAddonRel::getProductId, productIds)
                .eq(BcProductAddonRel::getStatus, 1));
        return rels.stream().collect(Collectors.groupingBy(BcProductAddonRel::getProductId));
    }

    private StoreMenuProductView buildProductView(
            BcProduct product,
            List<BcProductSku> skus,
            List<BcProductSpecGroup> specGroups,
            Map<Long, List<BcProductSpecOption>> specOptionMap,
            List<BcProductAttrGroupRel> attrGroupRels,
            Map<Long, BcProductAttrGroup> attrGroupMap,
            Map<Long, List<BcProductAttrOption>> attrOptionMap,
            List<BcProductAttrRel> attrRels,
            List<BcProductAddonGroupRel> addonGroupRels,
            Map<Long, BcAddonGroup> addonGroupMap,
            Map<Long, List<BcAddonItem>> addonItemMap,
            List<BcProductAddonRel> addonRels,
            LocalDateTime now
    ) {
        // 构建 SKU 视图
        List<StoreMenuSkuView> skuViews = buildSkuViews(skus);
        
        // 使用 UnifiedOptionGroupAssembler 构建统一选项组
        List<OptionGroupView> optionGroups = new ArrayList<>();
        
        // 1. 规格组（SPEC）
        if (!CollectionUtils.isEmpty(specGroups)) {
            List<BcProductSpecOption> allSpecOptions = specGroups.stream()
                    .map(BcProductSpecGroup::getId)
                    .map(specOptionMap::get)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            List<OptionGroupView> specOptionGroups = unifiedOptionGroupAssembler.assembleSpecGroups(specGroups, allSpecOptions);
            optionGroups.addAll(filterOptionGroups(specOptionGroups, now));
        }
        
        // 2. 属性组（ATTR）
        if (!CollectionUtils.isEmpty(attrGroupRels)) {
            List<BcProductAttrGroup> attrGroups = attrGroupRels.stream()
                    .map(BcProductAttrGroupRel::getAttrGroupId)
                    .map(attrGroupMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            List<BcProductAttrOption> allAttrOptions = attrGroups.stream()
                    .map(BcProductAttrGroup::getId)
                    .map(attrOptionMap::get)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            List<OptionGroupView> attrOptionGroups = unifiedOptionGroupAssembler.assembleAttrGroups(
                    attrGroups, attrGroupRels, allAttrOptions, attrRels);
            optionGroups.addAll(filterOptionGroups(attrOptionGroups, now));
        }
        
        // 3. 小料组（ADDON）
        if (!CollectionUtils.isEmpty(addonGroupRels)) {
            List<BcAddonGroup> addonGroups = addonGroupRels.stream()
                    .map(BcProductAddonGroupRel::getAddonGroupId)
                    .map(addonGroupMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            List<BcAddonItem> allAddonItems = addonGroups.stream()
                    .map(BcAddonGroup::getId)
                    .map(addonItemMap::get)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            List<OptionGroupView> addonOptionGroups = unifiedOptionGroupAssembler.assembleAddonGroups(
                    addonGroups, addonGroupRels, allAddonItems, addonRels);
            optionGroups.addAll(filterOptionGroups(addonOptionGroups, now));
        }

        return StoreMenuProductView.builder()
                .productId(product.getId())
                .name(product.getName())
                .subtitle(product.getSubtitle())
                .mainImage(product.getMainImage())
                .tags(Collections.emptyList()) // TODO: 如需标签，从 product.getTags() 获取
                .productMeta(parseProductMeta(product.getProductMeta()))
                .skus(skuViews)
                .optionGroups(optionGroups)
                .build();
    }

    private List<StoreMenuSkuView> buildSkuViews(List<BcProductSku> skus) {
        if (CollectionUtils.isEmpty(skus)) {
            return Collections.emptyList();
        }
        return skus.stream()
                .sorted(Comparator.comparing(BcProductSku::getSortOrder, Comparator.reverseOrder())
                        .thenComparing(BcProductSku::getId))
                .map(sku -> StoreMenuSkuView.builder()
                        .skuId(sku.getId())
                        .name(sku.getName())
                        .price(sku.getBasePrice())
                        .originPrice(defaultIfNull(sku.getMarketPrice(), sku.getBasePrice()))
                        .defaultSku(sku.getIsDefault() != null && sku.getIsDefault())
                        .ext(parseProductMeta(sku.getSkuMeta()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 过滤选项组：仅保留启用且在展示窗口内的组和项。
     */
    private List<OptionGroupView> filterOptionGroups(List<OptionGroupView> optionGroups, LocalDateTime now) {
        if (CollectionUtils.isEmpty(optionGroups)) {
            return Collections.emptyList();
        }
        return optionGroups.stream()
                .filter(group -> group.getEnabled() && isInDisplayWindow(group.getDisplayStartAt(), group.getDisplayEndAt(), now))
                .peek(group -> {
                    // 过滤选项/小料项：仅保留启用的
                    if (group.getItems() != null) {
                        List<com.bluecone.app.product.dto.view.unified.OptionItemView> filteredItems = group.getItems().stream()
                                .filter(com.bluecone.app.product.dto.view.unified.OptionItemView::getEnabled)
                                .collect(Collectors.toList());
                        group.setItems(filteredItems);
                    }
                })
                .filter(group -> !CollectionUtils.isEmpty(group.getItems())) // 过滤掉没有有效选项的组
                .collect(Collectors.toList());
    }

    /**
     * 判断是否在展示窗口内。
     * <p>
     * 规则：
     * <ul>
     *   <li>start 为空视为立即生效</li>
     *   <li>end 为空视为永久有效</li>
     *   <li>now 在 [start, end] 内才展示</li>
     * </ul>
     */
    private boolean isInDisplayWindow(LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        boolean afterStart = start == null || !now.isBefore(start);
        boolean beforeEnd = end == null || now.isBefore(end);
        return afterStart && beforeEnd;
    }

    private boolean isEnabled(Integer status) {
        return status != null && status == 1;
    }

    private BigDecimal defaultIfNull(BigDecimal value, BigDecimal fallback) {
        return value != null ? value : fallback;
    }

    private Map<String, Object> parseProductMeta(String metaJson) {
        if (metaJson == null || metaJson.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(metaJson, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("解析 productMeta 失败: {}", metaJson, e);
            return Collections.emptyMap();
        }
    }
}
