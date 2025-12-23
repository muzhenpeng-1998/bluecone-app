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
import java.util.Set;

/**
 * 门店菜单快照缓存失效辅助工具（Prompt 09 + 门店级 Epoch Keying）。
 * <p>
 * 职责：
 * <ul>
 *   <li>封装菜单快照缓存失效事件的发布逻辑</li>
 *   <li>使用门店级 Epoch Keying 机制进行精准失效（避免租户级全量失效）</li>
 *   <li>确保事件在事务提交后发送（afterCommit）</li>
 * </ul>
 * <p>
 * <b>门店级 Epoch 设计：</b>
 * <ul>
 *   <li>每个门店使用独立的 namespace：CacheNamespaces.STORE_MENU_SNAPSHOT + ":" + storeId</li>
 *   <li>修改某个门店的菜单时，只会 bump 该门店的 epoch，不影响同租户其他门店</li>
 *   <li>避免租户级全量失效，大幅提升缓存命中率</li>
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
     * <b>门店级 Epoch 失效策略：</b>
     * <ul>
     *   <li>使用门店级 namespace：CacheNamespaces.STORE_MENU_SNAPSHOT + ":" + storeId</li>
     *   <li>只 bump 该门店的 epoch，不影响同租户其他门店</li>
     *   <li>发布 CacheInvalidationEvent 时使用同一个门店级 namespace，让集群其他实例也 bump 该门店的 epoch</li>
     * </ul>
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
            // 构建门店级 namespace：store:menu:snap:{storeId}
            // 这样每个门店有独立的 epoch，避免租户级全量失效
            String storeNamespace = CacheNamespaces.STORE_MENU_SNAPSHOT + ":" + storeId;
            
            // 使用门店级 Epoch Bump 机制失效该门店的缓存
            if (epochProvider != null) {
                long newEpoch = epochProvider.bumpEpoch(tenantId, storeNamespace);
                log.debug("门店菜单快照缓存已失效（门店级 Epoch Bump）: tenantId={}, storeId={}, namespace={}, newEpoch={}, reason={}", 
                        tenantId, storeId, storeNamespace, newEpoch, reason);
            }
            
            // 发布 epoch bump 事件，通知集群其他实例也 bump 该门店的 epoch
            // 注意：必须使用同一个门店级 namespace
            CacheInvalidationEvent event = new CacheInvalidationEvent(
                    idService.nextUlid().toString(),
                    tenantId,
                    InvalidationScope.STORE,
                    storeNamespace, // 使用门店级 namespace
                    Collections.emptyList(), // 不使用 DIRECT_KEYS
                    0L, // configVersion
                    Instant.now(),
                    true, // epochBump = true
                    null, // newEpoch 由接收方自行 bump
                    "EPOCH_BUMP" // protectionHint
            );

            cacheInvalidationPublisher.publishAfterCommit(event);

            log.debug("门店菜单快照缓存失效事件已发布（门店级 Epoch Bump）: tenantId={}, storeId={}, namespace={}, reason={}", 
                    tenantId, storeId, storeNamespace, reason);
        } catch (Exception ex) {
            // best-effort: 不影响主流程
            log.error("门店菜单快照缓存失效失败: tenantId={}, storeId={}, reason={}", 
                    tenantId, storeId, reason, ex);
        }
    }

    /**
     * 失效多个门店的菜单快照缓存（批量失效）。
     * <p>
     * <b>门店级 Epoch 失效策略：</b>
     * <ul>
     *   <li>对每个门店分别调用 invalidateStoreMenu，使用独立的门店级 namespace</li>
     *   <li>只 bump 受影响门店的 epoch，不影响其他门店</li>
     * </ul>
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

        log.debug("批量失效门店菜单快照缓存: tenantId={}, storeCount={}, reason={}", 
                tenantId, storeIds.size(), reason);
        
        for (Long storeId : storeIds) {
            invalidateStoreMenu(tenantId, storeId, reason);
        }
    }

    /**
     * 失效租户下所有门店的菜单快照缓存（租户级失效）。
     * <p>
     * <b>注意：</b>此方法已废弃，不再推荐使用。
     * <p>
     * <b>原因：</b>门店级 Epoch 设计下，每个门店使用独立的 namespace，
     * 无法通过单一 namespace bump 失效所有门店。
     * <p>
     * <b>替代方案：</b>
     * <ul>
     *   <li>如果知道受影响的门店列表，使用 {@link #invalidateStoreMenus(Long, Set, String)}</li>
     *   <li>如果不知道受影响的门店，应在 Coordinator 层查询受影响门店后逐个失效</li>
     * </ul>
     * <p>
     * <b>当前实现：</b>为了向后兼容，此方法仍然保留，但实际上不会失效任何缓存（因为没有门店使用基础 namespace）。
     * 调用方应改为使用 {@link #invalidateStoreMenus(Long, Set, String)}。
     *
     * @param tenantId 租户ID
     * @param reason   失效原因（用于日志和监控）
     * @deprecated 请使用 {@link #invalidateStoreMenus(Long, Set, String)} 代替
     */
    @Deprecated
    public void invalidateTenantMenus(Long tenantId, String reason) {
        if (tenantId == null) {
            log.warn("无效的参数，跳过菜单快照缓存失效: tenantId={}", tenantId);
            return;
        }

        log.warn("invalidateTenantMenus 已废弃，门店级 Epoch 设计下无法通过单一 namespace 失效所有门店。" +
                "请改用 invalidateStoreMenus 并传入受影响的门店列表。tenantId={}, reason={}", 
                tenantId, reason);
        
        // 为了向后兼容，仍然 bump 基础 namespace，但实际上不会失效任何缓存
        // 因为所有门店都使用 "store:menu:snap:{storeId}" 作为 namespace
        try {
            if (epochProvider != null) {
                long newEpoch = epochProvider.bumpEpoch(tenantId, CacheNamespaces.STORE_MENU_SNAPSHOT);
                log.debug("租户菜单快照缓存基础 namespace 已 bump（但不会失效任何门店缓存）: tenantId={}, namespace={}, newEpoch={}, reason={}", 
                        tenantId, CacheNamespaces.STORE_MENU_SNAPSHOT, newEpoch, reason);
            }
        } catch (Exception ex) {
            log.error("租户菜单快照缓存失效失败: tenantId={}, reason={}", tenantId, reason, ex);
        }
    }
}

