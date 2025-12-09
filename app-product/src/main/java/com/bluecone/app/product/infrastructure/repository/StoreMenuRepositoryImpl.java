package com.bluecone.app.product.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.product.dao.entity.BcProduct;
import com.bluecone.app.product.dao.entity.BcProductCategory;
import com.bluecone.app.product.dao.entity.BcProductCategoryRel;
import com.bluecone.app.product.dao.entity.BcProductSku;
import com.bluecone.app.product.dao.entity.BcProductStoreConfig;
import com.bluecone.app.product.dao.mapper.BcProductCategoryMapper;
import com.bluecone.app.product.dao.mapper.BcProductCategoryRelMapper;
import com.bluecone.app.product.dao.mapper.BcProductMapper;
import com.bluecone.app.product.dao.mapper.BcProductSkuMapper;
import com.bluecone.app.product.dao.mapper.BcProductStoreConfigMapper;
import com.bluecone.app.product.domain.model.menu.StoreSkuSnapshot;
import com.bluecone.app.product.domain.model.menu.UserStoreCategory;
import com.bluecone.app.product.domain.model.menu.UserStoreMenu;
import com.bluecone.app.product.domain.model.menu.UserStoreMenuItem;
import com.bluecone.app.product.domain.repository.StoreMenuRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StoreMenuRepositoryImpl implements StoreMenuRepository {

    private final BcProductStoreConfigMapper storeConfigMapper;
    private final BcProductSkuMapper skuMapper;
    private final BcProductMapper productMapper;
    private final BcProductCategoryRelMapper categoryRelMapper;
    private final BcProductCategoryMapper categoryMapper;

    @Override
    public UserStoreMenu loadUserStoreMenu(Long tenantId, Long storeId) {
        List<BcProductStoreConfig> configs = storeConfigMapper.selectList(
                new LambdaQueryWrapper<BcProductStoreConfig>()
                        .eq(BcProductStoreConfig::getTenantId, tenantId)
                        .eq(BcProductStoreConfig::getStoreId, storeId)
                        .eq(BcProductStoreConfig::getStatus, 1)
                        .eq(BcProductStoreConfig::getVisible, true)
                        .isNotNull(BcProductStoreConfig::getSkuId));
        if (configs == null || configs.isEmpty()) {
            return UserStoreMenu.empty(tenantId, storeId);
        }
        Set<Long> skuIds = new LinkedHashSet<>();
        Set<Long> productIds = new LinkedHashSet<>();
        for (BcProductStoreConfig config : configs) {
            if (config.getSkuId() != null) {
                skuIds.add(config.getSkuId());
            }
            if (config.getProductId() != null) {
                productIds.add(config.getProductId());
            }
        }
        if (skuIds.isEmpty()) {
            return UserStoreMenu.empty(tenantId, storeId);
        }
        List<BcProductSku> skus = skuMapper.selectBatchIds(new ArrayList<>(skuIds));
        Map<Long, BcProductSku> skuMap = skus.stream()
                .filter(sku -> sku != null && sku.getStatus() != null && sku.getStatus() == 1)
                .collect(Collectors.toMap(BcProductSku::getId, sku -> sku));
        if (skuMap.isEmpty()) {
            return UserStoreMenu.empty(tenantId, storeId);
        }
        Map<Long, BcProduct> productMap = Collections.emptyMap();
        if (!productIds.isEmpty()) {
            List<BcProduct> products = productMapper.selectBatchIds(new ArrayList<>(productIds));
            if (products != null && !products.isEmpty()) {
                productMap = products.stream()
                        .filter(prod -> prod != null && prod.getStatus() != null && prod.getStatus() == 1)
                        .collect(Collectors.toMap(BcProduct::getId, prod -> prod));
            }
        }
        Map<Long, List<BcProductCategoryRel>> relsByProduct;
        if (productIds.isEmpty()) {
            relsByProduct = Collections.emptyMap();
        } else {
            relsByProduct = categoryRelMapper.selectList(
                            new LambdaQueryWrapper<BcProductCategoryRel>()
                                    .eq(BcProductCategoryRel::getTenantId, tenantId)
                                    .in(BcProductCategoryRel::getProductId, productIds)
                                    .eq(BcProductCategoryRel::getStatus, 1))
                    .stream()
                    .collect(Collectors.groupingBy(BcProductCategoryRel::getProductId));
        }
        Set<Long> categoryIds = relsByProduct.values().stream()
                .flatMap(List::stream)
                .map(BcProductCategoryRel::getCategoryId)
                .collect(Collectors.toSet());
        Map<Long, BcProductCategory> categoryMap;
        if (categoryIds.isEmpty()) {
            categoryMap = Collections.emptyMap();
        } else {
            categoryMap = categoryMapper.selectList(
                            new LambdaQueryWrapper<BcProductCategory>()
                                    .eq(BcProductCategory::getTenantId, tenantId)
                                    .in(BcProductCategory::getId, categoryIds)
                                    .eq(BcProductCategory::getStatus, 1))
                    .stream()
                    .collect(Collectors.toMap(BcProductCategory::getId, cat -> cat));
        }

        Map<Long, List<UserStoreMenuItem>> itemsByCategory = new LinkedHashMap<>();
        Map<Long, Integer> categoryOrder = new HashMap<>();

        for (BcProductStoreConfig config : configs) {
            if (config.getSkuId() == null) {
                continue;
            }
            BcProductSku sku = skuMap.get(config.getSkuId());
            if (sku == null) {
                continue;
            }
            Long productId = sku.getProductId();
            List<BcProductCategoryRel> rels = relsByProduct.getOrDefault(productId, Collections.emptyList());
            Long categoryId = rels.stream()
                    .map(BcProductCategoryRel::getCategoryId)
                    .filter(categoryMap::containsKey)
                    .findFirst()
                    .orElse(null);
            if (categoryId == null) {
                continue;
            }
            BcProductCategory category = categoryMap.get(categoryId);
            if (category == null) {
                continue;
            }
            long salePrice = toCents(config.getOverridePrice() != null ? config.getOverridePrice() : sku.getBasePrice());
            boolean available = Boolean.TRUE.equals(config.getVisible())
                    && Integer.valueOf(1).equals(config.getStatus())
                    && sku.getStatus() != null && sku.getStatus() == 1;
            BcProduct product = productMap.get(productId);
            UserStoreMenuItem item = UserStoreMenuItem.builder()
                    .skuId(sku.getId())
                    .skuName(sku.getName())
                    .skuShortName(sku.getName())
                    .imageUrl(product != null ? product.getMainImage() : null)
                    .salePrice(salePrice)
                    .available(available)
                    .displayOrder(config.getSortOrder())
                    .build();
            itemsByCategory.computeIfAbsent(categoryId, id -> new ArrayList<>()).add(item);
            categoryOrder.putIfAbsent(categoryId, category.getSortOrder());
        }

        List<UserStoreCategory> categories = itemsByCategory.entrySet().stream()
                .map(entry -> {
                    Long categoryId = entry.getKey();
                    BcProductCategory category = categoryMap.get(categoryId);
                    List<UserStoreMenuItem> categoryItems = entry.getValue();
                    categoryItems.sort(Comparator.comparingInt(item -> item.getDisplayOrder() == null ? 0 : -item.getDisplayOrder()));
                    return UserStoreCategory.builder()
                            .categoryId(categoryId)
                            .categoryName(category != null ? category.getName() : null)
                            .displayOrder(categoryOrder.getOrDefault(categoryId, 0))
                            .items(categoryItems)
                            .build();
                })
                .sorted(Comparator.comparingInt(category ->
                        category.getDisplayOrder() == null ? 0 : -category.getDisplayOrder()))
                .collect(Collectors.toList());

        return UserStoreMenu.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .categories(categories)
                .build();
    }

    @Override
    public Map<Long, StoreSkuSnapshot> loadStoreSkuSnapshotMap(Long tenantId, Long storeId, Collection<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BcProductStoreConfig> configs = storeConfigMapper.selectList(
                new LambdaQueryWrapper<BcProductStoreConfig>()
                        .eq(BcProductStoreConfig::getTenantId, tenantId)
                        .eq(BcProductStoreConfig::getStoreId, storeId)
                        .in(BcProductStoreConfig::getSkuId, skuIds)
                        .eq(BcProductStoreConfig::getStatus, 1));
        if (configs == null || configs.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = configs.stream()
                .map(BcProductStoreConfig::getSkuId)
                .distinct()
                .collect(Collectors.toList());
        List<BcProductSku> skus = skuMapper.selectBatchIds(ids);
        Map<Long, BcProductSku> skuMap = skus.stream()
                .filter(sku -> sku != null && sku.getStatus() != null && sku.getStatus() == 1)
                .collect(Collectors.toMap(BcProductSku::getId, sku -> sku));
        Map<Long, StoreSkuSnapshot> snapshots = new HashMap<>();
        for (BcProductStoreConfig config : configs) {
            Long skuId = config.getSkuId();
            if (skuId == null || snapshots.containsKey(skuId)) {
                continue;
            }
            BcProductSku sku = skuMap.get(skuId);
            if (sku == null) {
                continue;
            }
            long salePrice = toCents(config.getOverridePrice() != null ? config.getOverridePrice() : sku.getBasePrice());
            boolean available = Boolean.TRUE.equals(config.getVisible())
                    && Integer.valueOf(1).equals(config.getStatus())
                    && sku.getStatus() != null && sku.getStatus() == 1;
            snapshots.put(skuId, StoreSkuSnapshot.builder()
                    .skuId(skuId)
                    .tenantId(tenantId)
                    .storeId(storeId)
                    .productId(sku.getProductId())
                    .skuName(sku.getName())
                    .salePrice(salePrice)
                    .available(available)
                    .build());
        }
        return snapshots;
    }

    private long toCents(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }
}
