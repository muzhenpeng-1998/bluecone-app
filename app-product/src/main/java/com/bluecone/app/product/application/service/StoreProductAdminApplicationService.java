package com.bluecone.app.product.application.service;

import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;
import com.bluecone.app.core.cacheinval.api.CacheInvalidationPublisher;
import com.bluecone.app.core.cacheinval.api.InvalidationScope;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.product.application.dto.StoreProductReorderRequest;
import com.bluecone.app.product.application.dto.StoreProductVisibilityRequest;
import com.bluecone.app.product.dao.entity.BcProduct;
import com.bluecone.app.product.dao.entity.BcProductStoreConfig;
import com.bluecone.app.product.dao.mapper.BcProductMapper;
import com.bluecone.app.product.dao.mapper.BcProductStoreConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 门店商品管理应用服务
 * 
 * <h3>📋 职责范围：</h3>
 * <ul>
 *   <li>商品上架/下架（控制门店可见性）</li>
 *   <li>商品排序（门店维度）</li>
 *   <li>缓存失效（菜单快照）</li>
 * </ul>
 * 
 * <h3>💡 设计原则：</h3>
 * <ul>
 *   <li><b>门店维度</b>：所有操作都基于 tenant_id + store_id + product_id + channel</li>
 *   <li><b>缓存失效</b>：任何变更都失效对应门店的菜单快照</li>
 *   <li><b>幂等性</b>：重复上架/下架不报错</li>
 * </ul>
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoreProductAdminApplicationService {
    
    private final BcProductMapper productMapper;
    private final BcProductStoreConfigMapper storeConfigMapper;
    private final IdService idService;
    
    @Autowired(required = false)
    @Nullable
    private CacheInvalidationPublisher cacheInvalidationPublisher;
    
    @Autowired(required = false)
    @Nullable
    private com.bluecone.app.product.infrastructure.cache.MenuSnapshotInvalidationHelper menuSnapshotInvalidationHelper;
    
    @Autowired(required = false)
    @Nullable
    private com.bluecone.app.product.domain.service.StoreMenuSnapshotDomainService storeMenuSnapshotDomainService;
    
    /**
     * 设置商品在门店的可见性（上架/下架）
     * 
     * <p>如果配置不存在，则创建；如果存在，则更新。
     * 
     * @param storeId 门店ID
     * @param productId 商品ID
     * @param request 可见性设置请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void setProductVisibility(Long storeId, Long productId, StoreProductVisibilityRequest request) {
        Long tenantId = request.getTenantId();
        Long operatorId = request.getOperatorId();
        String channel = request.getChannel() != null ? request.getChannel().toUpperCase() : "ALL";
        
        log.info("设置商品可见性: tenantId={}, storeId={}, productId={}, visible={}, channel={}", 
                tenantId, storeId, productId, request.getVisible(), channel);
        
        // 1. 校验商品是否存在
        BcProduct product = productMapper.selectOne(new LambdaQueryWrapper<BcProduct>()
                .eq(BcProduct::getId, productId)
                .eq(BcProduct::getTenantId, tenantId)
                .eq(BcProduct::getDeleted, 0));
        
        if (product == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "商品不存在或无权访问");
        }
        
        // 2. 查询是否已有配置
        BcProductStoreConfig existing = storeConfigMapper.selectOne(new LambdaQueryWrapper<BcProductStoreConfig>()
                .eq(BcProductStoreConfig::getTenantId, tenantId)
                .eq(BcProductStoreConfig::getStoreId, storeId)
                .eq(BcProductStoreConfig::getProductId, productId)
                .eq(BcProductStoreConfig::getChannel, channel)
                .eq(request.getSkuId() != null, BcProductStoreConfig::getSkuId, request.getSkuId())
                .isNull(request.getSkuId() == null, BcProductStoreConfig::getSkuId));
        
        if (existing == null) {
            // 3.1 创建新配置
            Long configId = idService.nextLong(IdScope.PRODUCT);
            
            BcProductStoreConfig config = new BcProductStoreConfig();
            config.setId(configId);
            config.setTenantId(tenantId);
            config.setStoreId(storeId);
            config.setProductId(productId);
            config.setSkuId(request.getSkuId());
            config.setChannel(channel);
            config.setVisible(request.getVisible());
            config.setStatus(1); // 启用
            config.setSortOrder(0);
            config.setDisplayStartAt(request.getDisplayStartAt());
            config.setDisplayEndAt(request.getDisplayEndAt());
            config.setDeleted(0);
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            config.setCreatedBy(operatorId);
            config.setUpdatedBy(operatorId);
            
            storeConfigMapper.insert(config);
            log.info("商品门店配置已创建: configId={}", configId);
        } else {
            // 3.2 更新现有配置
            existing.setVisible(request.getVisible());
            existing.setDisplayStartAt(request.getDisplayStartAt());
            existing.setDisplayEndAt(request.getDisplayEndAt());
            existing.setUpdatedAt(LocalDateTime.now());
            existing.setUpdatedBy(operatorId);
            
            storeConfigMapper.updateById(existing);
            log.info("商品门店配置已更新: configId={}", existing.getId());
        }
        
        // 4. afterCommit：发布缓存失效事件
        publishStoreMenuSnapshotInvalidation(tenantId, storeId);
        
        // 5. 可选：自动重建快照
        if (Boolean.TRUE.equals(request.getAutoRebuildSnapshot())) {
            rebuildStoreMenuSnapshot(tenantId, storeId, channel);
        }
        
        log.info("商品可见性设置成功: tenantId={}, storeId={}, productId={}, visible={}, autoRebuild={}", 
                tenantId, storeId, productId, request.getVisible(), request.getAutoRebuildSnapshot());
    }
    
    /**
     * 批量调整商品在门店的排序
     * 
     * @param storeId 门店ID
     * @param request 排序请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void reorderProducts(Long storeId, StoreProductReorderRequest request) {
        Long tenantId = request.getTenantId();
        Long operatorId = request.getOperatorId();
        String channel = request.getChannel() != null ? request.getChannel().toUpperCase() : "ALL";
        
        log.info("批量调整商品排序: tenantId={}, storeId={}, channel={}, count={}", 
                tenantId, storeId, channel, request.getProducts().size());
        
        // 1. 批量更新排序
        for (StoreProductReorderRequest.ProductSortItem item : request.getProducts()) {
            // 查询配置是否存在
            BcProductStoreConfig config = storeConfigMapper.selectOne(new LambdaQueryWrapper<BcProductStoreConfig>()
                    .eq(BcProductStoreConfig::getTenantId, tenantId)
                    .eq(BcProductStoreConfig::getStoreId, storeId)
                    .eq(BcProductStoreConfig::getProductId, item.getProductId())
                    .eq(BcProductStoreConfig::getChannel, channel)
                    .isNull(BcProductStoreConfig::getSkuId));
            
            if (config == null) {
                // 如果配置不存在，创建一个（自动上架）
                Long configId = idService.nextLong(IdScope.PRODUCT);
                
                config = new BcProductStoreConfig();
                config.setId(configId);
                config.setTenantId(tenantId);
                config.setStoreId(storeId);
                config.setProductId(item.getProductId());
                config.setChannel(channel);
                config.setVisible(true); // 自动上架
                config.setStatus(1);
                config.setSortOrder(item.getSortOrder());
                config.setDeleted(0);
                config.setCreatedAt(LocalDateTime.now());
                config.setUpdatedAt(LocalDateTime.now());
                config.setCreatedBy(operatorId);
                config.setUpdatedBy(operatorId);
                
                storeConfigMapper.insert(config);
            } else {
                // 更新排序
                config.setSortOrder(item.getSortOrder());
                config.setUpdatedAt(LocalDateTime.now());
                config.setUpdatedBy(operatorId);
                
                storeConfigMapper.updateById(config);
            }
        }
        
        // 2. afterCommit：发布缓存失效事件
        publishStoreMenuSnapshotInvalidation(tenantId, storeId);
        
        // 3. 可选：自动重建快照
        if (Boolean.TRUE.equals(request.getAutoRebuildSnapshot())) {
            rebuildStoreMenuSnapshot(tenantId, storeId, channel);
        }
        
        log.info("商品排序调整成功: tenantId={}, storeId={}, count={}, autoRebuild={}", 
                tenantId, storeId, request.getProducts().size(), request.getAutoRebuildSnapshot());
    }
    
    /**
     * 发布门店菜单快照失效事件（Prompt 09：使用 MenuSnapshotInvalidationHelper）。
     * <p>
     * 按 tenant+store 失效（细粒度）
     */
    private void publishStoreMenuSnapshotInvalidation(Long tenantId, Long storeId) {
        if (menuSnapshotInvalidationHelper == null || tenantId == null || storeId == null) {
            log.warn("MenuSnapshotInvalidationHelper 未注入或参数为空，跳过菜单快照失效");
            return;
        }
        
        // Prompt 09: 门店上架/下架/排序，按门店失效（细粒度）
        menuSnapshotInvalidationHelper.invalidateStoreMenu(tenantId, storeId, "门店商品配置变更");
    }
    
    /**
     * 重建门店菜单快照（可选功能，Phase 4 增强）
     * <p>
     * 立即重建指定门店/渠道的菜单快照
     */
    private void rebuildStoreMenuSnapshot(Long tenantId, Long storeId, String channel) {
        if (storeMenuSnapshotDomainService == null) {
            log.warn("StoreMenuSnapshotDomainService 未注入，跳过快照重建");
            return;
        }
        
        try {
            String channelCode = channel != null ? channel.toUpperCase() : "ALL";
            String orderScene = "DEFAULT"; // 默认场景
            LocalDateTime now = LocalDateTime.now();
            
            log.info("开始重建门店菜单快照: tenantId={}, storeId={}, channel={}, orderScene={}", 
                    tenantId, storeId, channelCode, orderScene);
            
            storeMenuSnapshotDomainService.rebuildAndSaveSnapshot(tenantId, storeId, channelCode, orderScene, now);
            
            log.info("门店菜单快照重建成功: tenantId={}, storeId={}, channel={}", 
                    tenantId, storeId, channelCode);
        } catch (Exception ex) {
            // best-effort: 不影响主流程
            log.error("门店菜单快照重建失败: tenantId={}, storeId={}, channel={}", 
                    tenantId, storeId, channel, ex);
        }
    }
}

