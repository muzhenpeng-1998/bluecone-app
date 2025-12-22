package com.bluecone.app.product.domain.service;

import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
import com.bluecone.app.core.contextkit.CacheNamespaces;
import com.bluecone.app.product.dao.entity.BcStoreMenuSnapshot;
import com.bluecone.app.product.domain.model.menu.StoreMenuSnapshotModel;
import com.bluecone.app.product.domain.repository.StoreMenuSnapshotRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 菜单快照领域服务：负责"构建 + 落库"的完整流程。
 * <p>
 * Prompt 07 增强：
 * <ul>
 *   <li>支持 {@code now} 参数，用于定时展示过滤</li>
 *   <li>version 自增由 {@link StoreMenuSnapshotRepository} 自动处理</li>
 * </ul>
 * <p>后续可挂接商品/门店配置变更事件触发重建，也可在此扩展 Redis 缓存写入。</p>
 *
 * @author System
 * @since 2025-12-21
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoreMenuSnapshotDomainService {

    private final StoreMenuSnapshotBuilderService builderService;
    private final StoreMenuSnapshotRepository storeMenuSnapshotRepository;
    private final CacheEpochProvider epochProvider;
    
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.lang.Nullable
    private com.bluecone.app.product.infrastructure.cache.MenuSnapshotInvalidationHelper menuSnapshotInvalidationHelper;

    /**
     * 重建并保存指定门店/渠道/场景的菜单快照。
     * <p>
     * Prompt 07: 添加 {@code now} 参数，用于定时展示过滤。
     *
     * @param tenantId   租户ID
     * @param storeId    门店ID
     * @param channel    渠道（ALL, DINE_IN, TAKEAWAY, DELIVERY, PICKUP）
     * @param orderScene 订单场景（DEFAULT, BREAKFAST, LUNCH, DINNER, NIGHT）
     * @param now        当前时间，用于定时展示判断（为 null 时使用 LocalDateTime.now()）
     * @return 保存后的快照实体
     */
    public BcStoreMenuSnapshot rebuildAndSaveSnapshot(
            Long tenantId, 
            Long storeId, 
            String channel, 
            String orderScene,
            LocalDateTime now
    ) {
        // 构建菜单快照模型
        StoreMenuSnapshotModel model = builderService.buildStoreMenuSnapshot(tenantId, storeId, channel, orderScene, now);
        String menuJson = builderService.buildMenuJson(model);
        
        // 创建快照实体
        BcStoreMenuSnapshot entity = new BcStoreMenuSnapshot();
        entity.setTenantId(tenantId);
        entity.setStoreId(storeId);
        entity.setChannel(channel);
        entity.setOrderScene(orderScene);
        entity.setMenuJson(menuJson);
        entity.setGeneratedAt(now != null ? now : LocalDateTime.now());
        entity.setStatus(1);
        
        // 保存或更新快照（version 自增由 Repository 自动处理）
        storeMenuSnapshotRepository.saveOrUpdateSnapshot(entity);
        
        // 查询最新快照
        BcStoreMenuSnapshot latest = storeMenuSnapshotRepository
                .findByTenantAndStoreAndChannelAndScene(tenantId, storeId, channel, orderScene)
                .orElse(entity);
        
        // Prompt 08: 失效缓存（使用 Epoch Keying，namespace 级失效）
        if (epochProvider != null) {
            try {
                long newEpoch = epochProvider.bumpEpoch(tenantId, CacheNamespaces.STORE_MENU_SNAPSHOT);
                log.info("菜单快照缓存已失效（Epoch Bump）: tenantId={}, namespace={}, newEpoch={}", 
                        tenantId, CacheNamespaces.STORE_MENU_SNAPSHOT, newEpoch);
            } catch (Exception ex) {
                log.error("菜单快照缓存失效失败: tenantId={}, storeId={}", tenantId, storeId, ex);
                // best-effort: 不影响主流程
            }
        }
        
        // Phase 5 增强：同时使用 MenuSnapshotInvalidationHelper 失效门店级缓存
        if (menuSnapshotInvalidationHelper != null) {
            try {
                menuSnapshotInvalidationHelper.invalidateStoreMenu(tenantId, storeId, "snapshot rebuilt");
                log.info("门店菜单缓存已失效（MenuSnapshotInvalidationHelper）: tenantId={}, storeId={}", 
                        tenantId, storeId);
            } catch (Exception ex) {
                log.error("门店菜单缓存失效失败: tenantId={}, storeId={}", tenantId, storeId, ex);
                // best-effort: 不影响主流程
            }
        }
        
        log.info("菜单快照已重建并保存, tenantId={}, storeId={}, channel={}, scene={}, version={}", 
                tenantId, storeId, channel, orderScene, latest.getVersion());
        
        return latest;
    }
    
    /**
     * 查询指定门店/渠道/场景的菜单快照（只读，不触发重建）。
     * <p>
     * Phase 5 新增：GET 语义修正，只查询现有快照，不执行重建。
     *
     * @param tenantId   租户ID
     * @param storeId    门店ID
     * @param channel    渠道（ALL, DINE_IN, TAKEAWAY, DELIVERY, PICKUP）
     * @param orderScene 订单场景（DEFAULT, BREAKFAST, LUNCH, DINNER, NIGHT）
     * @return 快照实体，如果不存在返回 null
     */
    public BcStoreMenuSnapshot getSnapshot(
            Long tenantId, 
            Long storeId, 
            String channel, 
            String orderScene
    ) {
        log.info("查询菜单快照（只读）: tenantId={}, storeId={}, channel={}, orderScene={}", 
                tenantId, storeId, channel, orderScene);
        
        return storeMenuSnapshotRepository
                .findByTenantAndStoreAndChannelAndScene(tenantId, storeId, channel, orderScene)
                .orElse(null);
    }

    /**
     * 重建并保存指定门店/渠道/场景的菜单快照（使用当前时间）。
     * <p>
     * 向后兼容方法，默认使用当前时间。
     *
     * @param tenantId   租户ID
     * @param storeId    门店ID
     * @param channel    渠道
     * @param orderScene 订单场景
     * @return 保存后的快照实体
     */
    public BcStoreMenuSnapshot rebuildAndSaveSnapshot(Long tenantId, Long storeId, String channel, String orderScene) {
        return rebuildAndSaveSnapshot(tenantId, storeId, channel, orderScene, null);
    }
}
