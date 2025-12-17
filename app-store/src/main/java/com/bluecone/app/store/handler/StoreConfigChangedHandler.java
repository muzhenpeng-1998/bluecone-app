package com.bluecone.app.store.handler;

import com.bluecone.app.core.event.EventHandler;
import com.bluecone.app.core.event.annotations.EventHandlerComponent;
import com.bluecone.app.store.event.StoreConfigChangedEvent;
import com.bluecone.app.store.infrastructure.cache.StoreConfigCache;
import com.bluecone.app.store.infrastructure.cache.StoreContextCache;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 应用内门店配置变更事件消费者。
 *
 * <p>通过实现 {@link EventHandler} 接收 {@link StoreConfigChangedEvent}，
 * 由 Outbox 路由器自动发现并在事件发布后回调此处理器。</p>
 *
 * <p>职责说明：</p>
 * <ul>
 *     <li>触发本地缓存失效（StoreConfigCache、StoreContextCache），确保后续读取使用最新配置</li>
 *     <li>可选：触发搜索索引同步、下游系统通知等业务逻辑</li>
 * </ul>
 *
 * <p>说明：缓存失效已在 {@link com.bluecone.app.store.application.service.StoreConfigChangeServiceImpl}
 * 中完成（包括 Redis 缓存失效），此处主要处理本地缓存和多级缓存的失效。</p>
 */
@EventHandlerComponent
@RequiredArgsConstructor
public class StoreConfigChangedHandler implements EventHandler<StoreConfigChangedEvent> {

    private static final Logger log = LoggerFactory.getLogger(StoreConfigChangedHandler.class);

    private final StoreConfigCache storeConfigCache;
    private final StoreContextCache storeContextCache;

    /**
     * 处理门店配置变更事件。
     * <p>说明：此方法在配置变更事件发布后被调用，用于处理事件消费后的业务逻辑（如缓存失效、索引同步等）。</p>
     *
     * @param event 门店配置变更事件
     */
    @Override
    public void handle(final StoreConfigChangedEvent event) {
        Long tenantId = event.getTenantId();
        Long storeId = event.getStoreId();
        Long configVersion = event.getConfigVersion();

        log.info("[StoreConfigChangedHandler] tenantId={} storeId={} configVersion={} eventId={}",
                tenantId, storeId, configVersion, event.getEventId());

        // 1. 失效本地缓存（StoreConfigCache、StoreContextCache）
        // 说明：虽然 StoreConfigChangeServiceImpl 已失效 Redis 缓存，但本地缓存需要在此处失效，
        // 确保多级缓存的一致性，避免后续读取到旧版本配置
        try {
            storeConfigCache.evictStore(tenantId, storeId);
            storeContextCache.evictStore(tenantId, storeId);
            log.debug("[StoreConfigChangedHandler] 已失效本地缓存 tenantId={} storeId={}", tenantId, storeId);
        } catch (Exception ex) {
            log.warn("[StoreConfigChangedHandler] 失效本地缓存失败 tenantId={} storeId={}", tenantId, storeId, ex);
        }

        // 2. 可选：触发搜索索引同步
        // 说明：如门店配置变更需要同步到搜索系统（如 Elasticsearch），可在此处调用搜索服务
        // searchService.syncStore(tenantId, storeId);

        // 3. 可选：触发下游系统通知
        // 说明：如需要通知外部系统（如第三方平台、BI 系统等），可在此处调用通知服务
        // notificationService.notifyStoreConfigChanged(tenantId, storeId, configVersion);
    }
}

