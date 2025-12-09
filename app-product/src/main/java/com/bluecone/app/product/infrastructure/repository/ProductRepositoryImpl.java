package com.bluecone.app.product.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.bluecone.app.product.dao.entity.BcStoreMenuSnapshot;
import com.bluecone.app.product.dao.mapper.BcAddonGroupMapper;
import com.bluecone.app.product.dao.mapper.BcAddonItemMapper;
import com.bluecone.app.product.dao.mapper.BcProductAddonRelMapper;
import com.bluecone.app.product.dao.mapper.BcProductAttrGroupMapper;
import com.bluecone.app.product.dao.mapper.BcProductAttrOptionMapper;
import com.bluecone.app.product.dao.mapper.BcProductAttrRelMapper;
import com.bluecone.app.product.dao.mapper.BcProductCategoryMapper;
import com.bluecone.app.product.dao.mapper.BcProductCategoryRelMapper;
import com.bluecone.app.product.dao.mapper.BcProductMapper;
import com.bluecone.app.product.dao.mapper.BcProductSkuMapper;
import com.bluecone.app.product.dao.mapper.BcProductSpecGroupMapper;
import com.bluecone.app.product.dao.mapper.BcProductSpecOptionMapper;
import com.bluecone.app.product.dao.mapper.BcProductStoreConfigMapper;
import com.bluecone.app.product.dao.mapper.BcProductTagMapper;
import com.bluecone.app.product.dao.mapper.BcProductTagRelMapper;
import com.bluecone.app.product.dao.mapper.BcStoreMenuSnapshotMapper;
import com.bluecone.app.product.domain.enums.MenuScene;
import com.bluecone.app.product.domain.enums.ProductStatus;
import com.bluecone.app.product.domain.enums.SaleChannel;
import com.bluecone.app.product.domain.model.Product;
import com.bluecone.app.product.domain.model.ProductSku;
import com.bluecone.app.product.domain.model.readmodel.StoreMenuSnapshot;
import com.bluecone.app.product.domain.repository.ProductRepository;
import com.bluecone.app.product.infrastructure.assembler.ProductConfigAssembler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

/**
 * 商品聚合的领域仓储实现，对标 app-store，实现多表组装的高并发读模型。
 * <p>高隔离：对领域层暴露语义化方法，屏蔽 MyBatis-Plus 与表结构细节。</p>
 * <p>高并发：一次性组装完整 Product 聚合，可直接作为缓存值使用。</p>
 */
@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final BcProductMapper productMapper;
    private final BcProductSkuMapper productSkuMapper;
    private final BcProductCategoryMapper productCategoryMapper;
    private final BcProductCategoryRelMapper productCategoryRelMapper;
    private final BcProductSpecGroupMapper productSpecGroupMapper;
    private final BcProductSpecOptionMapper productSpecOptionMapper;
    private final BcProductAttrGroupMapper productAttrGroupMapper;
    private final BcProductAttrOptionMapper productAttrOptionMapper;
    private final BcProductAttrRelMapper productAttrRelMapper;
    private final BcAddonGroupMapper addonGroupMapper;
    private final BcAddonItemMapper addonItemMapper;
    private final BcProductAddonRelMapper productAddonRelMapper;
    private final BcProductTagMapper productTagMapper;
    private final BcProductTagRelMapper productTagRelMapper;
    private final BcProductStoreConfigMapper productStoreConfigMapper;
    private final BcStoreMenuSnapshotMapper storeMenuSnapshotMapper;
    private final ProductConfigAssembler productConfigAssembler;

    /**
     * 高并发读路径：加载单个商品在指定门店与渠道下的完整聚合（可直接缓存）。
     */
    @Override
    public Product loadProductAggregate(Long tenantId, Long productId, Long storeId, String channel) {
        String channelCode = channel == null ? null : channel.toUpperCase();

        BcProduct product = productMapper.selectOne(new LambdaQueryWrapper<BcProduct>()
                .eq(BcProduct::getTenantId, tenantId)
                .eq(BcProduct::getId, productId)
                .eq(BcProduct::getStatus, 1));
        if (product == null) {
            return null;
        }

        List<BcProductSku> skus = productSkuMapper.selectList(new LambdaQueryWrapper<BcProductSku>()
                .eq(BcProductSku::getTenantId, tenantId)
                .eq(BcProductSku::getProductId, productId)
                .eq(BcProductSku::getStatus, 1));

        List<BcProductSpecGroup> specGroups = productSpecGroupMapper.selectList(new LambdaQueryWrapper<BcProductSpecGroup>()
                .eq(BcProductSpecGroup::getTenantId, tenantId)
                .eq(BcProductSpecGroup::getProductId, productId)
                .eq(BcProductSpecGroup::getStatus, 1));
        List<BcProductSpecOption> specOptions = productSpecOptionMapper.selectList(new LambdaQueryWrapper<BcProductSpecOption>()
                .eq(BcProductSpecOption::getTenantId, tenantId)
                .eq(BcProductSpecOption::getProductId, productId)
                .eq(BcProductSpecOption::getStatus, 1));

        List<BcProductAttrRel> attrRels = productAttrRelMapper.selectList(new LambdaQueryWrapper<BcProductAttrRel>()
                .eq(BcProductAttrRel::getTenantId, tenantId)
                .eq(BcProductAttrRel::getProductId, productId)
                .eq(BcProductAttrRel::getStatus, 1));
        Set<Long> attrGroupIds = attrRels.stream()
                .map(BcProductAttrRel::getAttrGroupId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        List<BcProductAttrGroup> attrGroups = CollectionUtils.isEmpty(attrGroupIds) ? Collections.emptyList()
                : productAttrGroupMapper.selectList(new LambdaQueryWrapper<BcProductAttrGroup>()
                .eq(BcProductAttrGroup::getTenantId, tenantId)
                .in(BcProductAttrGroup::getId, attrGroupIds)
                .eq(BcProductAttrGroup::getStatus, 1));
        List<BcProductAttrOption> attrOptions = CollectionUtils.isEmpty(attrGroupIds) ? Collections.emptyList()
                : productAttrOptionMapper.selectList(new LambdaQueryWrapper<BcProductAttrOption>()
                .eq(BcProductAttrOption::getTenantId, tenantId)
                .in(BcProductAttrOption::getAttrGroupId, attrGroupIds)
                .eq(BcProductAttrOption::getStatus, 1));

        List<BcProductAddonRel> addonRels = productAddonRelMapper.selectList(new LambdaQueryWrapper<BcProductAddonRel>()
                .eq(BcProductAddonRel::getTenantId, tenantId)
                .eq(BcProductAddonRel::getProductId, productId)
                .eq(BcProductAddonRel::getStatus, 1));
        Set<Long> addonGroupIds = addonRels.stream()
                .map(BcProductAddonRel::getAddonGroupId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        List<BcAddonGroup> addonGroups = CollectionUtils.isEmpty(addonGroupIds) ? Collections.emptyList()
                : addonGroupMapper.selectList(new LambdaQueryWrapper<BcAddonGroup>()
                .eq(BcAddonGroup::getTenantId, tenantId)
                .in(BcAddonGroup::getId, addonGroupIds)
                .eq(BcAddonGroup::getStatus, 1));
        List<BcAddonItem> addonItems = CollectionUtils.isEmpty(addonGroupIds) ? Collections.emptyList()
                : addonItemMapper.selectList(new LambdaQueryWrapper<BcAddonItem>()
                .eq(BcAddonItem::getTenantId, tenantId)
                .in(BcAddonItem::getGroupId, addonGroupIds)
                .eq(BcAddonItem::getStatus, 1));

        List<BcProductTagRel> tagRels = productTagRelMapper.selectList(new LambdaQueryWrapper<BcProductTagRel>()
                .eq(BcProductTagRel::getTenantId, tenantId)
                .eq(BcProductTagRel::getProductId, productId)
                .eq(BcProductTagRel::getStatus, 1));
        Set<Long> tagIds = tagRels.stream()
                .map(BcProductTagRel::getTagId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        List<BcProductTag> tags = CollectionUtils.isEmpty(tagIds) ? Collections.emptyList()
                : productTagMapper.selectList(new LambdaQueryWrapper<BcProductTag>()
                .eq(BcProductTag::getTenantId, tenantId)
                .in(BcProductTag::getId, tagIds)
                .eq(BcProductTag::getStatus, 1));

        List<BcProductCategoryRel> categoryRels = productCategoryRelMapper.selectList(new LambdaQueryWrapper<BcProductCategoryRel>()
                .eq(BcProductCategoryRel::getTenantId, tenantId)
                .eq(BcProductCategoryRel::getProductId, productId)
                .eq(BcProductCategoryRel::getStatus, 1));
        Set<Long> categoryIds = categoryRels.stream()
                .map(BcProductCategoryRel::getCategoryId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        List<BcProductCategory> categories = CollectionUtils.isEmpty(categoryIds) ? Collections.emptyList()
                : productCategoryMapper.selectList(new LambdaQueryWrapper<BcProductCategory>()
                .eq(BcProductCategory::getTenantId, tenantId)
                .in(BcProductCategory::getId, categoryIds)
                .eq(BcProductCategory::getStatus, 1));

        LambdaQueryWrapper<BcProductStoreConfig> storeConfigWrapper = new LambdaQueryWrapper<BcProductStoreConfig>()
                .eq(BcProductStoreConfig::getTenantId, tenantId)
                .eq(BcProductStoreConfig::getProductId, productId)
                .eq(BcProductStoreConfig::getVisible, true)
                .eq(BcProductStoreConfig::getStatus, 1);
        if (storeId != null) {
            storeConfigWrapper.eq(BcProductStoreConfig::getStoreId, storeId);
        }
        if (channelCode != null) {
            storeConfigWrapper.in(BcProductStoreConfig::getChannel, List.of("ALL", channelCode));
        }
        List<BcProductStoreConfig> storeConfigs = productStoreConfigMapper.selectList(storeConfigWrapper);

        return productConfigAssembler.assembleProductAggregate(
                product,
                skus,
                specGroups,
                specOptions,
                attrRels,
                attrGroups,
                attrOptions,
                addonRels,
                addonGroups,
                addonItems,
                tagRels,
                tags,
                categoryRels,
                categories,
                storeConfigs
        );
    }

    /**
     * 高并发读路径：按门店+渠道加载可售商品列表（当前实现循环查询，后续可优化成批量聚合）。
     */
    @Override
    public List<Product> loadAvailableProductsForStore(Long tenantId, Long storeId, String channel) {
        String channelCode = channel == null ? null : channel.toUpperCase();
        List<BcProductStoreConfig> configs = productStoreConfigMapper.selectList(new LambdaQueryWrapper<BcProductStoreConfig>()
                .eq(BcProductStoreConfig::getTenantId, tenantId)
                .eq(storeId != null, BcProductStoreConfig::getStoreId, storeId)
                .eq(BcProductStoreConfig::getStatus, 1)
                .eq(BcProductStoreConfig::getVisible, true)
                .in(channelCode != null, BcProductStoreConfig::getChannel, List.of("ALL", channelCode)));
        if (CollectionUtils.isEmpty(configs)) {
            return Collections.emptyList();
        }
        Set<Long> productIds = configs.stream().map(BcProductStoreConfig::getProductId).collect(Collectors.toSet());
        List<Product> result = new ArrayList<>();
        for (Long productId : productIds) {
            Product aggregate = loadProductAggregate(tenantId, productId, storeId, channel);
            if (aggregate != null) {
                result.add(aggregate);
            }
        }
        return result;
    }

    /**
     * 读取门店菜单快照，支撑小程序/前端的高并发菜单拉取。
     */
    @Override
    public StoreMenuSnapshot loadStoreMenuSnapshot(Long tenantId, Long storeId, String channel, String orderScene) {
        BcStoreMenuSnapshot entity = storeMenuSnapshotMapper.selectOne(new LambdaQueryWrapper<BcStoreMenuSnapshot>()
                .eq(BcStoreMenuSnapshot::getTenantId, tenantId)
                .eq(BcStoreMenuSnapshot::getStoreId, storeId)
                .eq(channel != null, BcStoreMenuSnapshot::getChannel, channel)
                .eq(orderScene != null, BcStoreMenuSnapshot::getOrderScene, orderScene)
                .eq(BcStoreMenuSnapshot::getStatus, 1));
        if (entity == null) {
            return null;
        }
        return StoreMenuSnapshot.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .storeId(entity.getStoreId())
                .channel(SaleChannel.fromCode(entity.getChannel()))
                .scene(MenuScene.fromCode(entity.getOrderScene()))
                .version(entity.getVersion())
                .menuJson(entity.getMenuJson())
                .generatedAt(entity.getGeneratedAt())
                .status(ProductStatus.fromCode(entity.getStatus()))
                .build();
    }

    /**
     * 门店菜单快照的简单 upsert，实现高并发读写分离的存储入口。
     */
    @Override
    public void saveOrUpdateStoreMenuSnapshot(StoreMenuSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        BcStoreMenuSnapshot existing = storeMenuSnapshotMapper.selectOne(new LambdaQueryWrapper<BcStoreMenuSnapshot>()
                .eq(BcStoreMenuSnapshot::getTenantId, snapshot.getTenantId())
                .eq(BcStoreMenuSnapshot::getStoreId, snapshot.getStoreId())
                .eq(snapshot.getChannel() != null, BcStoreMenuSnapshot::getChannel, snapshot.getChannel() == null ? null : snapshot.getChannel().getCode())
                .eq(snapshot.getScene() != null, BcStoreMenuSnapshot::getOrderScene, snapshot.getScene() == null ? null : snapshot.getScene().getCode()));

        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            BcStoreMenuSnapshot entity = new BcStoreMenuSnapshot();
            entity.setTenantId(snapshot.getTenantId());
            entity.setStoreId(snapshot.getStoreId());
            entity.setChannel(snapshot.getChannel() == null ? null : snapshot.getChannel().getCode());
            entity.setOrderScene(snapshot.getScene() == null ? null : snapshot.getScene().getCode());
            entity.setVersion(snapshot.getVersion() != null ? snapshot.getVersion() : 1L);
            entity.setMenuJson(snapshot.getMenuJson());
            entity.setGeneratedAt(snapshot.getGeneratedAt() == null ? now : snapshot.getGeneratedAt());
            entity.setStatus(snapshot.getStatus() != null ? snapshot.getStatus().getCode() : ProductStatus.ENABLED.getCode());
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            storeMenuSnapshotMapper.insert(entity);
        } else {
            existing.setMenuJson(snapshot.getMenuJson());
            existing.setVersion(snapshot.getVersion() != null ? snapshot.getVersion() : existing.getVersion());
            existing.setGeneratedAt(snapshot.getGeneratedAt() == null ? now : snapshot.getGeneratedAt());
            existing.setStatus(snapshot.getStatus() != null ? snapshot.getStatus().getCode() : existing.getStatus());
            existing.setUpdatedAt(now);
            storeMenuSnapshotMapper.updateById(existing);
        }
    }

    @Override
    public Optional<Product> findById(Long tenantId, Long productId) {
        return Optional.ofNullable(loadProductAggregate(tenantId, productId, null, null));
    }

    @Override
    public Optional<Product> findByCode(Long tenantId, String productCode) {
        BcProduct product = productMapper.selectOne(new LambdaQueryWrapper<BcProduct>()
                .eq(BcProduct::getTenantId, tenantId)
                .eq(BcProduct::getProductCode, productCode)
                .eq(BcProduct::getStatus, 1));
        if (product == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(loadProductAggregate(tenantId, product.getId(), null, null));
    }

    @Override
    public List<Product> findByIds(Long tenantId, Collection<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return Collections.emptyList();
        }
        return productIds.stream()
                .map(id -> loadProductAggregate(tenantId, id, null, null))
                .filter(item -> item != null)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ProductSku> findSkuById(Long tenantId, Long skuId) {
        if (skuId == null) {
            return Optional.empty();
        }
        BcProductSku entity = productSkuMapper.selectOne(new LambdaQueryWrapper<BcProductSku>()
                .eq(BcProductSku::getId, skuId)
                .eq(tenantId != null, BcProductSku::getTenantId, tenantId));
        return Optional.ofNullable(toSkuModel(entity));
    }

    @Override
    public Long save(Product product) {
        BcProduct entity = toEntity(product);
        productMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Product product) {
        if (product == null || product.getId() == null) {
            return;
        }
        LambdaUpdateWrapper<BcProduct> wrapper = new LambdaUpdateWrapper<BcProduct>()
                .eq(BcProduct::getId, product.getId())
                .eq(product.getTenantId() != null, BcProduct::getTenantId, product.getTenantId());
        if (product.getProductCode() != null) {
            wrapper.set(BcProduct::getProductCode, product.getProductCode());
        }
        if (product.getName() != null) {
            wrapper.set(BcProduct::getName, product.getName());
        }
        if (product.getSubtitle() != null) {
            wrapper.set(BcProduct::getSubtitle, product.getSubtitle());
        }
        if (product.getProductType() != null) {
            wrapper.set(BcProduct::getProductType, product.getProductType().getCode());
        }
        if (product.getDescription() != null) {
            wrapper.set(BcProduct::getDescription, product.getDescription());
        }
        if (product.getMainImage() != null) {
            wrapper.set(BcProduct::getMainImage, product.getMainImage());
        }
        if (product.getMediaGallery() != null) {
            wrapper.set(BcProduct::getMediaGallery, toJson(product.getMediaGallery()));
        }
        if (product.getUnit() != null) {
            wrapper.set(BcProduct::getUnit, product.getUnit());
        }
        if (product.getStatus() != null) {
            wrapper.set(BcProduct::getStatus, product.getStatus().getCode());
        }
        if (product.getSortOrder() != null) {
            wrapper.set(BcProduct::getSortOrder, product.getSortOrder());
        }
        if (product.getProductMeta() != null) {
            wrapper.set(BcProduct::getProductMeta, toJson(product.getProductMeta()));
        }
        wrapper.set(BcProduct::getUpdatedAt, LocalDateTime.now());
        productMapper.update(null, wrapper);
    }

    @Override
    public void changeStatus(Long tenantId, Long productId, ProductStatus newStatus, Long operatorId) {
        productMapper.update(null, new LambdaUpdateWrapper<BcProduct>()
                .eq(BcProduct::getTenantId, tenantId)
                .eq(BcProduct::getId, productId)
                .set(BcProduct::getStatus, newStatus == null ? null : newStatus.getCode())
                .set(BcProduct::getUpdatedBy, operatorId)
                .set(BcProduct::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public void updateSkuStatus(Long tenantId, Long skuId, ProductStatus status, Long operatorId) {
        if (skuId == null) {
            return;
        }
        productSkuMapper.update(null, new LambdaUpdateWrapper<BcProductSku>()
                .eq(BcProductSku::getId, skuId)
                .eq(tenantId != null, BcProductSku::getTenantId, tenantId)
                .set(BcProductSku::getStatus, status == null ? null : status.getCode())
                .set(BcProductSku::getUpdatedBy, operatorId)
                .set(BcProductSku::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public void updateSkuPrice(Long tenantId, Long skuId, BigDecimal newPrice, Long operatorId) {
        if (skuId == null) {
            return;
        }
        productSkuMapper.update(null, new LambdaUpdateWrapper<BcProductSku>()
                .eq(BcProductSku::getId, skuId)
                .eq(tenantId != null, BcProductSku::getTenantId, tenantId)
                .set(BcProductSku::getBasePrice, newPrice)
                .set(BcProductSku::getUpdatedBy, operatorId)
                .set(BcProductSku::getUpdatedAt, LocalDateTime.now()));
    }

    private BcProduct toEntity(Product product) {
        if (product == null) {
            return null;
        }
        BcProduct entity = new BcProduct();
        entity.setTenantId(product.getTenantId());
        entity.setProductCode(product.getProductCode());
        entity.setName(product.getName());
        entity.setSubtitle(product.getSubtitle());
        entity.setProductType(product.getProductType() == null ? null : product.getProductType().getCode());
        entity.setDescription(product.getDescription());
        entity.setMainImage(product.getMainImage());
        entity.setMediaGallery(toJson(product.getMediaGallery()));
        entity.setUnit(product.getUnit());
        entity.setStatus(product.getStatus() == null ? null : product.getStatus().getCode());
        entity.setSortOrder(product.getSortOrder());
        entity.setProductMeta(toJson(product.getProductMeta()));
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    private ProductSku toSkuModel(BcProductSku entity) {
        if (entity == null) {
            return null;
        }
        return ProductSku.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .productId(entity.getProductId())
                .skuCode(entity.getSkuCode())
                .name(entity.getName())
                .basePrice(entity.getBasePrice())
                .marketPrice(entity.getMarketPrice())
                .costPrice(entity.getCostPrice())
                .barcode(entity.getBarcode())
                .defaultSku(Boolean.TRUE.equals(entity.getIsDefault()))
                .status(ProductStatus.fromCode(entity.getStatus()))
                .sortOrder(entity.getSortOrder())
                .build();
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
