package com.bluecone.app.product.infrastructure.cache;

import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;
import com.bluecone.app.core.cacheinval.api.CacheInvalidationPublisher;
import com.bluecone.app.core.cacheinval.api.InvalidationScope;
import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
import com.bluecone.app.core.contextkit.CacheNamespaces;
import com.bluecone.app.id.api.IdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 门店菜单快照缓存失效辅助工具（Prompt 09 + Epoch Keying）。
 * <p>
 * 职责：
 * <ul>
 *   <li>封装菜单快照缓存失效事件的发布逻辑</li>
 *   <li>使用 Epoch Keying 机制进行 namespace 级失效（推荐）</li>
 *   <li>确保事件在事务提交后发送（afterCommit）</li>
 * </ul>
 * <p>
 * 使用场景：
 * <ul>
 *   <li>分类 create/update/reorder/status</li>
 *   <li>商品 create/update/status</li>
 *   <li>SKU 价格/排序更新</li>
 *   <li>绑定分类/属性组/小料组（含 overrides）</li>
 *   <li>门店上架/下架/门店排序</li>
 * </ul>
 * <p>
 * <b>重要变更：</b>从 DIRECT_KEYS 模式切换到 Epoch Bump 模式，避免无效 key 通配问题。
 *
 * @author System
 * @since 2025-12-21
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MenuSnapshotInvalidationHelper {

    private final CacheInvalidationPublisher cacheInvalidationPublisher;
    private final IdService idService;
    
    @Autowired(required = false)
    @Nullable
    private CacheEpochProvider epochProvider;

    /**
     * 失效指定门店的菜单快照缓存（单个门店）。
     * <p>
     * 失效策略：使用 Epoch Bump 机制，bump 整个 STORE_MENU_SNAPSHOT namespace 的 epoch。
     * <p>
     * 事件将在事务提交后发送（afterCommit）。
     *
     * @param tenantId 租户ID
     * @param storeId  门店ID
     * @param reason   失效原因（用于日志和监控）
     */
    public void invalidateStoreMenu(Long tenantId, Long storeId, String reason) {
        if (tenantId == null || storeId == null) {
            log.warn("无效的参数，跳过菜单快照缓存失效: tenantId={}, storeId={}", tenantId, storeId);
            return;
        }

        try {
            // 使用 Epoch Bump 机制失效整个 namespace
            if (epochProvider != null) {
                long newEpoch = epochProvider.bumpEpoch(tenantId, CacheNamespaces.STORE_MENU_SNAPSHOT);
                log.debug("菜单快照缓存已失效（Epoch Bump）: tenantId={}, storeId={}, namespace={}, newEpoch={}, reason={}", 
                        tenantId, storeId, CacheNamespaces.STORE_MENU_SNAPSHOT, newEpoch, reason);
            }
            
            // 同时发布 epoch bump 事件，通知其他实例
            CacheInvalidationEvent event = new CacheInvalidationEvent(
                    idService.nextUlid().toString(),
                    tenantId,
                    InvalidationScope.STORE,
                    CacheNamespaces.STORE_MENU_SNAPSHOT,
                    Collections.emptyList(), // 不使用 DIRECT_KEYS
                    0L, // configVersion
                    Instant.now(),
                    true, // epochBump = true
                    null, // newEpoch 由接收方自行 bump
                    "EPOCH_BUMP" // protectionHint
            );

            cacheInvalidationPublisher.publishAfterCommit(event);

            log.debug("菜单快照缓存失效事件已发布（Epoch Bump）: tenantId={}, storeId={}, reason={}", 
                    tenantId, storeId, reason);
        } catch (Exception ex) {
            // best-effort: 不影响主流程
            log.error("菜单快照缓存失效失败: tenantId={}, storeId={}, reason={}", 
                    tenantId, storeId, reason, ex);
        }
    }

    /**
     * 失效多个门店的菜单快照缓存（批量失效）。
     * <p>
     * 失效粒度：按 storeId 失效 STORE_MENU_SNAPSHOT。
     * <p>
     * 事件将在事务提交后发送（afterCommit）。
     *
     * @param tenantId 租户ID
     * @param storeIds 门店ID列表
     * @param reason   失效原因（用于日志和监控）
     */
    public void invalidateStoreMenus(Long tenantId, Set<Long> storeIds, String reason) {
        if (tenantId == null || storeIds == null || storeIds.isEmpty()) {
            log.warn("无效的参数，跳过菜单快照缓存失效: tenantId={}, storeIds={}", tenantId, storeIds);
            return;
        }

        for (Long storeId : storeIds) {
            invalidateStoreMenu(tenantId, storeId, reason);
        }
    }

    /**
     * 失效租户下所有门店的菜单快照缓存（租户级失效）。
     * <p>
     * 使用场景：分类/商品变更时，如果不知道影响哪些门店，则失效租户下所有门店。
     * <p>
     * 失效策略：使用 Epoch Bump 机制，bump 整个 STORE_MENU_SNAPSHOT namespace 的 epoch。
     * <p>
     * 注意：这是一个粗粒度的失效策略，会导致租户下所有门店的菜单缓存失效。
     * 建议优先使用 {@link #invalidateStoreMenu(Long, Long, String)} 或 {@link #invalidateStoreMenus(Long, Set, String)}。
     *
     * @param tenantId 租户ID
     * @param reason   失效原因（用于日志和监控）
     */
    public void invalidateTenantMenus(Long tenantId, String reason) {
        if (tenantId == null) {
            log.warn("无效的参数，跳过菜单快照缓存失效: tenantId={}", tenantId);
            return;
        }

        try {
            // 使用 Epoch Bump 机制失效整个 namespace
            if (epochProvider != null) {
                long newEpoch = epochProvider.bumpEpoch(tenantId, CacheNamespaces.STORE_MENU_SNAPSHOT);
                log.debug("租户菜单快照缓存已失效（Epoch Bump）: tenantId={}, namespace={}, newEpoch={}, reason={}", 
                        tenantId, CacheNamespaces.STORE_MENU_SNAPSHOT, newEpoch, reason);
            }
            
            // 同时发布 epoch bump 事件，通知其他实例
            CacheInvalidationEvent event = new CacheInvalidationEvent(
                    idService.nextUlid().toString(),
                    tenantId,
                    InvalidationScope.STORE,
                    CacheNamespaces.STORE_MENU_SNAPSHOT,
                    Collections.emptyList(), // 不使用 DIRECT_KEYS
                    0L, // configVersion
                    Instant.now(),
                    true, // epochBump = true
                    null, // newEpoch 由接收方自行 bump
                    "EPOCH_BUMP" // protectionHint
            );

            cacheInvalidationPublisher.publishAfterCommit(event);

            log.debug("租户菜单快照缓存失效事件已发布（Epoch Bump）: tenantId={}, reason={}", tenantId, reason);
        } catch (Exception ex) {
            // best-effort: 不影响主流程
            log.error("租户菜单快照缓存失效失败: tenantId={}, reason={}", tenantId, reason, ex);
        }
    }
}

