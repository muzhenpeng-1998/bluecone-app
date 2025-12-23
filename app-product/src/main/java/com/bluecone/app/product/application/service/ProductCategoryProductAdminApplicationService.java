package com.bluecone.app.product.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.product.dao.entity.BcProductCategoryRel;
import com.bluecone.app.product.dao.entity.BcProductStoreConfig;
import com.bluecone.app.product.dao.mapper.BcProductCategoryRelMapper;
import com.bluecone.app.product.dao.mapper.BcProductStoreConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 分类内商品管理应用服务（批量排序）。
 * <p>
 * <b>职责：</b>
 * <ul>
 *   <li>批量调整分类内商品的排序值（更新 bc_product_category_rel.sort_order）</li>
 *   <li>触发受影响门店的菜单快照重建（afterCommit）</li>
 * </ul>
 * <p>
 * <b>门店级 Epoch 设计：</b>
 * <ul>
 *   <li>只重建"上架了这些商品的门店"，避免全量重建</li>
 *   <li>通过 MenuSnapshotRebuildCoordinator 对每个受影响门店分别失效和重建</li>
 * </ul>
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCategoryProductAdminApplicationService {
    
    private final BcProductCategoryRelMapper productCategoryRelMapper;
    private final BcProductStoreConfigMapper productStoreConfigMapper;
    private final MenuSnapshotRebuildCoordinator menuSnapshotRebuildCoordinator;
    
    /**
     * 批量调整分类内商品的排序值。
     * <p>
     * <b>实现策略：</b>
     * <ol>
     *   <li>更新 bc_product_category_rel.sort_order（必须 tenantId + categoryId + productId 三条件）</li>
     *   <li>afterCommit：查询"上架了这些商品的门店"，对每个门店分别失效和重建</li>
     * </ol>
     * <p>
     * <b>注意：</b>如果商品未上架到任何门店，则不会触发重建。
     * 
     * @param tenantId 租户ID
     * @param categoryId 分类ID
     * @param reorderItems 排序项列表（productId + sortOrder）
     * @param operatorId 操作人ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void reorderCategoryProducts(
            Long tenantId, 
            Long categoryId, 
            List<ReorderItem> reorderItems, 
            Long operatorId) {
        
        if (tenantId == null || categoryId == null || reorderItems == null || reorderItems.isEmpty()) {
            throw new IllegalArgumentException("参数不能为空");
        }
        
        log.info("批量调整分类内商品排序: tenantId={}, categoryId={}, itemCount={}, operatorId={}", 
                tenantId, categoryId, reorderItems.size(), operatorId);
        
        // 1. 更新 bc_product_category_rel.sort_order
        LocalDateTime now = LocalDateTime.now();
        int updateCount = 0;
        
        for (ReorderItem item : reorderItems) {
            Long productId = item.getProductId();
            Integer sortOrder = item.getSortOrder();
            
            if (productId == null || sortOrder == null) {
                log.warn("跳过无效的排序项: productId={}, sortOrder={}", productId, sortOrder);
                continue;
            }
            
            // 更新排序值（必须 tenantId + categoryId + productId 三条件）
            int updated = productCategoryRelMapper.update(null, 
                    new LambdaUpdateWrapper<BcProductCategoryRel>()
                            .eq(BcProductCategoryRel::getTenantId, tenantId)
                            .eq(BcProductCategoryRel::getCategoryId, categoryId)
                            .eq(BcProductCategoryRel::getProductId, productId)
                            .eq(BcProductCategoryRel::getStatus, 1)
                            .set(BcProductCategoryRel::getSortOrder, sortOrder)
                            .set(BcProductCategoryRel::getUpdatedAt, now));
            
            if (updated > 0) {
                updateCount++;
                log.debug("更新分类内商品排序: tenantId={}, categoryId={}, productId={}, sortOrder={}", 
                        tenantId, categoryId, productId, sortOrder);
            } else {
                log.warn("分类内商品关联不存在，跳过: tenantId={}, categoryId={}, productId={}", 
                        tenantId, categoryId, productId);
            }
        }
        
        log.info("分类内商品排序更新完成: tenantId={}, categoryId={}, updateCount={}", 
                tenantId, categoryId, updateCount);
        
        // 2. afterCommit：触发受影响门店的菜单快照重建
        if (updateCount > 0) {
            // 获取所有受影响的商品ID
            Set<Long> affectedProductIds = reorderItems.stream()
                    .map(ReorderItem::getProductId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
            
            // 查询"上架了这些商品的门店"
            Set<Long> affectedStoreIds = queryAffectedStores(tenantId, affectedProductIds);
            
            if (affectedStoreIds.isEmpty()) {
                log.info("分类内商品未上架到任何门店，跳过菜单快照重建: tenantId={}, categoryId={}, productIds={}", 
                        tenantId, categoryId, affectedProductIds);
            } else {
                log.info("触发受影响门店的菜单快照重建: tenantId={}, categoryId={}, storeCount={}", 
                        tenantId, categoryId, affectedStoreIds.size());
                
                // 对每个受影响门店分别失效和重建（门店级 Epoch）
                for (Long storeId : affectedStoreIds) {
                    menuSnapshotRebuildCoordinator.afterCommitRebuildForStore(
                            tenantId, storeId, "category-product:reorder");
                }
            }
        }
    }
    
    /**
     * 查询"上架了这些商品的门店"。
     * <p>
     * 从 bc_product_store_config 查询 distinct store_id（deleted=0 且 status=1 且 visible=1）。
     * 
     * @param tenantId 租户ID
     * @param productIds 商品ID列表
     * @return 门店ID列表
     */
    private Set<Long> queryAffectedStores(Long tenantId, Set<Long> productIds) {
        if (productIds.isEmpty()) {
            return Set.of();
        }
        
        List<BcProductStoreConfig> configs = productStoreConfigMapper.selectList(
                new LambdaQueryWrapper<BcProductStoreConfig>()
                        .eq(BcProductStoreConfig::getTenantId, tenantId)
                        .in(BcProductStoreConfig::getProductId, productIds)
                        .eq(BcProductStoreConfig::getDeleted, 0)
                        .eq(BcProductStoreConfig::getStatus, 1)
                        .eq(BcProductStoreConfig::getVisible, true)
                        .select(BcProductStoreConfig::getStoreId));
        
        return configs.stream()
                .map(BcProductStoreConfig::getStoreId)
                .collect(Collectors.toSet());
    }
    
    /**
     * 排序项。
     */
    public static class ReorderItem {
        private Long productId;
        private Integer sortOrder;
        
        public ReorderItem() {
        }
        
        public ReorderItem(Long productId, Integer sortOrder) {
            this.productId = productId;
            this.sortOrder = sortOrder;
        }
        
        public Long getProductId() {
            return productId;
        }
        
        public void setProductId(Long productId) {
            this.productId = productId;
        }
        
        public Integer getSortOrder() {
            return sortOrder;
        }
        
        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }
        
        @Override
        public String toString() {
            return "ReorderItem{productId=" + productId + ", sortOrder=" + sortOrder + "}";
        }
    }
}

