package com.bluecone.app.product.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
import com.bluecone.app.core.contextkit.CacheNamespaces;
import com.bluecone.app.product.dao.entity.BcProductStoreConfig;
import com.bluecone.app.product.dao.mapper.BcProductStoreConfigMapper;
import com.bluecone.app.product.domain.service.StoreMenuSnapshotDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单快照重建编排器 - afterCommit 自动重建快照。
 * <p>
 * <b>设计原则：</b>
 * <ul>
 *   <li><b>事务后执行</b>：必须在事务提交后执行（使用 {@link TransactionSynchronizationManager}）</li>
 *   <li><b>失败不影响主事务</b>：重建失败只记录日志，不抛异常影响主流程</li>
 *   <li><b>Epoch Bump</b>：重建完成后触发缓存失效（namespace 级别 Epoch bump）</li>
 *   <li><b>粒度可控</b>：支持按租户全量重建、按商品关联门店重建、按单门店重建</li>
 * </ul>
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *   <li>分类 create/update/reorder/status → {@link #afterCommitRebuildForTenant}</li>
 *   <li>商品 create/update/changeStatus → {@link #afterCommitRebuildForProduct}</li>
 *   <li>属性组/小料组 变更 → {@link #afterCommitRebuildForTenant}</li>
 *   <li>门店上架/下架/排序 → {@link #afterCommitRebuildForStore}</li>
 * </ul>
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MenuSnapshotRebuildCoordinator {
    
    private final StoreMenuSnapshotDomainService storeMenuSnapshotDomainService;
    private final BcProductStoreConfigMapper storeConfigMapper;
    
    @Nullable
    private final CacheEpochProvider epochProvider;
    
    /**
     * afterCommit：重建租户下所有门店的菜单快照（粗粒度）。
     * <p>
     * <b>适用场景：</b>分类/属性组/小料组变更（影响范围大）。
     * <p>
     * <b>实现策略：</b>
     * <ul>
     *   <li>从 {@code bc_product_store_config} 查询租户下所有 distinct store_id（deleted=0 且 status=1 且 visible=1）</li>
     *   <li>对每个 storeId 执行 {@code rebuildAndSaveSnapshot(tenantId, storeId, "ALL", "DEFAULT", now)}</li>
     *   <li>失败不影响主事务（best-effort）</li>
     *   <li>重建完成后触发 Epoch Bump</li>
     * </ul>
     * 
     * @param tenantId 租户ID
     * @param reason 触发原因（用于日志）
     */
    public void afterCommitRebuildForTenant(Long tenantId, String reason) {
        if (tenantId == null) {
            log.warn("afterCommitRebuildForTenant: tenantId 为空，跳过");
            return;
        }
        
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.warn("afterCommitRebuildForTenant: 当前无活跃事务，直接执行重建（不推荐）");
            rebuildForTenantInternal(tenantId, reason);
            return;
        }
        
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rebuildForTenantInternal(tenantId, reason);
            }
        });
        
        log.debug("afterCommitRebuildForTenant: 已注册 afterCommit 回调, tenantId={}, reason={}", tenantId, reason);
    }
    
    /**
     * afterCommit：重建指定商品关联的所有门店的菜单快照（中粒度）。
     * <p>
     * <b>适用场景：</b>商品 create/update/changeStatus（影响特定门店）。
     * <p>
     * <b>实现策略：</b>
     * <ul>
     *   <li>从 {@code bc_product_store_config} 查询该 productId 关联的 distinct store_id（deleted=0 且 status=1 且 visible=1）</li>
     *   <li>对每个 storeId 执行 {@code rebuildAndSaveSnapshot(tenantId, storeId, "ALL", "DEFAULT", now)}</li>
     *   <li>失败不影响主事务（best-effort）</li>
     *   <li>重建完成后触发 Epoch Bump</li>
     * </ul>
     * 
     * @param tenantId 租户ID
     * @param productId 商品ID
     * @param reason 触发原因（用于日志）
     */
    public void afterCommitRebuildForProduct(Long tenantId, Long productId, String reason) {
        if (tenantId == null || productId == null) {
            log.warn("afterCommitRebuildForProduct: tenantId 或 productId 为空，跳过");
            return;
        }
        
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.warn("afterCommitRebuildForProduct: 当前无活跃事务，直接执行重建（不推荐）");
            rebuildForProductInternal(tenantId, productId, reason);
            return;
        }
        
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rebuildForProductInternal(tenantId, productId, reason);
            }
        });
        
        log.debug("afterCommitRebuildForProduct: 已注册 afterCommit 回调, tenantId={}, productId={}, reason={}", 
                tenantId, productId, reason);
    }
    
    /**
     * afterCommit：重建指定门店的菜单快照（细粒度）。
     * <p>
     * <b>适用场景：</b>门店上架/下架/排序（影响单个门店）。
     * <p>
     * <b>实现策略：</b>
     * <ul>
     *   <li>直接执行 {@code rebuildAndSaveSnapshot(tenantId, storeId, "ALL", "DEFAULT", now)}</li>
     *   <li>失败不影响主事务（best-effort）</li>
     *   <li>重建完成后触发 Epoch Bump</li>
     * </ul>
     * 
     * @param tenantId 租户ID
     * @param storeId 门店ID
     * @param reason 触发原因（用于日志）
     */
    public void afterCommitRebuildForStore(Long tenantId, Long storeId, String reason) {
        if (tenantId == null || storeId == null) {
            log.warn("afterCommitRebuildForStore: tenantId 或 storeId 为空，跳过");
            return;
        }
        
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.warn("afterCommitRebuildForStore: 当前无活跃事务，直接执行重建（不推荐）");
            rebuildForStoreInternal(tenantId, storeId, reason);
            return;
        }
        
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rebuildForStoreInternal(tenantId, storeId, reason);
            }
        });
        
        log.debug("afterCommitRebuildForStore: 已注册 afterCommit 回调, tenantId={}, storeId={}, reason={}", 
                tenantId, storeId, reason);
    }
    
    // ===== 私有方法：实际执行重建逻辑 =====
    
    /**
     * 租户级重建逻辑实现。
     */
    private void rebuildForTenantInternal(Long tenantId, String reason) {
        try {
            log.info("开始重建租户菜单快照: tenantId={}, reason={}", tenantId, reason);
            
            // 1. 查询租户下所有 distinct store_id（deleted=0, status=1, visible=1）
            Set<Long> storeIds = queryDistinctStoresForTenant(tenantId);
            
            if (storeIds.isEmpty()) {
                log.info("租户下没有门店配置，跳过重建: tenantId={}", tenantId);
                // 即使没有门店，也要 bump epoch，避免旧缓存残留
                bumpEpochForTenant(tenantId);
                return;
            }
            
            log.info("查询到门店数量: tenantId={}, storeCount={}", tenantId, storeIds.size());
            
            // 2. 逐个门店重建快照
            int successCount = 0;
            int failureCount = 0;
            LocalDateTime now = LocalDateTime.now();
            
            for (Long storeId : storeIds) {
                try {
                    storeMenuSnapshotDomainService.rebuildAndSaveSnapshot(
                            tenantId, storeId, "ALL", "DEFAULT", now);
                    successCount++;
                } catch (Exception ex) {
                    log.error("重建门店菜单快照失败: tenantId={}, storeId={}, reason={}", 
                            tenantId, storeId, reason, ex);
                    failureCount++;
                    // 继续处理下一个门店
                }
            }
            
            log.info("租户菜单快照重建完成: tenantId={}, reason={}, successCount={}, failureCount={}", 
                    tenantId, reason, successCount, failureCount);
            
            // 3. 重建完成后触发 Epoch Bump（即使部分失败）
            bumpEpochForTenant(tenantId);
            
        } catch (Exception ex) {
            log.error("租户菜单快照重建失败: tenantId={}, reason={}", tenantId, reason, ex);
            // 失败不影响主流程
        }
    }
    
    /**
     * 商品级重建逻辑实现。
     */
    private void rebuildForProductInternal(Long tenantId, Long productId, String reason) {
        try {
            log.info("开始重建商品关联门店菜单快照: tenantId={}, productId={}, reason={}", 
                    tenantId, productId, reason);
            
            // 1. 查询该商品关联的 distinct store_id（deleted=0, status=1, visible=1）
            Set<Long> storeIds = queryDistinctStoresForProduct(tenantId, productId);
            
            if (storeIds.isEmpty()) {
                log.info("商品未关联任何门店，跳过重建: tenantId={}, productId={}", tenantId, productId);
                // 即使没有门店，也要 bump epoch
                bumpEpochForTenant(tenantId);
                return;
            }
            
            log.info("查询到门店数量: tenantId={}, productId={}, storeCount={}", 
                    tenantId, productId, storeIds.size());
            
            // 2. 逐个门店重建快照
            int successCount = 0;
            int failureCount = 0;
            LocalDateTime now = LocalDateTime.now();
            
            for (Long storeId : storeIds) {
                try {
                    storeMenuSnapshotDomainService.rebuildAndSaveSnapshot(
                            tenantId, storeId, "ALL", "DEFAULT", now);
                    successCount++;
                } catch (Exception ex) {
                    log.error("重建门店菜单快照失败: tenantId={}, storeId={}, productId={}, reason={}", 
                            tenantId, storeId, productId, reason, ex);
                    failureCount++;
                    // 继续处理下一个门店
                }
            }
            
            log.info("商品关联门店菜单快照重建完成: tenantId={}, productId={}, reason={}, successCount={}, failureCount={}", 
                    tenantId, productId, reason, successCount, failureCount);
            
            // 3. 重建完成后触发 Epoch Bump（即使部分失败）
            bumpEpochForTenant(tenantId);
            
        } catch (Exception ex) {
            log.error("商品关联门店菜单快照重建失败: tenantId={}, productId={}, reason={}", 
                    tenantId, productId, reason, ex);
            // 失败不影响主流程
        }
    }
    
    /**
     * 门店级重建逻辑实现。
     */
    private void rebuildForStoreInternal(Long tenantId, Long storeId, String reason) {
        try {
            log.info("开始重建门店菜单快照: tenantId={}, storeId={}, reason={}", tenantId, storeId, reason);
            
            LocalDateTime now = LocalDateTime.now();
            storeMenuSnapshotDomainService.rebuildAndSaveSnapshot(
                    tenantId, storeId, "ALL", "DEFAULT", now);
            
            log.info("门店菜单快照重建完成: tenantId={}, storeId={}, reason={}", tenantId, storeId, reason);
            
            // 重建完成后触发 Epoch Bump
            bumpEpochForTenant(tenantId);
            
        } catch (Exception ex) {
            log.error("门店菜单快照重建失败: tenantId={}, storeId={}, reason={}", 
                    tenantId, storeId, reason, ex);
            // 失败不影响主流程
        }
    }
    
    /**
     * 查询租户下所有 distinct store_id。
     */
    private Set<Long> queryDistinctStoresForTenant(Long tenantId) {
        List<BcProductStoreConfig> configs = storeConfigMapper.selectList(
                new LambdaQueryWrapper<BcProductStoreConfig>()
                        .eq(BcProductStoreConfig::getTenantId, tenantId)
                        .eq(BcProductStoreConfig::getDeleted, 0)
                        .eq(BcProductStoreConfig::getStatus, 1)
                        .eq(BcProductStoreConfig::getVisible, true)
                        .select(BcProductStoreConfig::getStoreId));
        
        return configs.stream()
                .map(BcProductStoreConfig::getStoreId)
                .collect(Collectors.toSet());
    }
    
    /**
     * 查询商品关联的 distinct store_id。
     */
    private Set<Long> queryDistinctStoresForProduct(Long tenantId, Long productId) {
        List<BcProductStoreConfig> configs = storeConfigMapper.selectList(
                new LambdaQueryWrapper<BcProductStoreConfig>()
                        .eq(BcProductStoreConfig::getTenantId, tenantId)
                        .eq(BcProductStoreConfig::getProductId, productId)
                        .eq(BcProductStoreConfig::getDeleted, 0)
                        .eq(BcProductStoreConfig::getStatus, 1)
                        .eq(BcProductStoreConfig::getVisible, true)
                        .select(BcProductStoreConfig::getStoreId));
        
        return configs.stream()
                .map(BcProductStoreConfig::getStoreId)
                .collect(Collectors.toSet());
    }
    
    /**
     * Bump 租户的 Epoch（触发缓存失效）。
     */
    private void bumpEpochForTenant(Long tenantId) {
        if (epochProvider == null) {
            log.warn("CacheEpochProvider 未注入，跳过 Epoch Bump");
            return;
        }
        
        try {
            long newEpoch = epochProvider.bumpEpoch(tenantId, CacheNamespaces.STORE_MENU_SNAPSHOT);
            log.info("菜单快照缓存 Epoch 已更新: tenantId={}, namespace={}, newEpoch={}", 
                    tenantId, CacheNamespaces.STORE_MENU_SNAPSHOT, newEpoch);
        } catch (Exception ex) {
            log.error("Epoch Bump 失败: tenantId={}, namespace={}", 
                    tenantId, CacheNamespaces.STORE_MENU_SNAPSHOT, ex);
            // 失败不影响主流程
        }
    }
}
