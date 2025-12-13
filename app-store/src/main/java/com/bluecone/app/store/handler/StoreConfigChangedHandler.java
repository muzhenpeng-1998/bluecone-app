package com.bluecone.app.store.handler;

import com.bluecone.app.core.event.EventHandler;
import com.bluecone.app.core.event.annotations.EventHandlerComponent;
import com.bluecone.app.store.event.StoreConfigChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 应用内门店配置变更事件消费者。
 *
 * <p>通过实现 {@link EventHandler} 接收 {@link StoreConfigChangedEvent}，
 * 由 Outbox 路由器自动发现并在事件发布后回调此处理器。</p>
 *
 * <p>当前实现仅记录关键日志，你可以在 {@link #handle(StoreConfigChangedEvent)}
 * 中注入并调用领域服务，完成搜索索引同步、下游更新等业务逻辑。</p>
 */
@EventHandlerComponent
public class StoreConfigChangedHandler implements EventHandler<StoreConfigChangedEvent> {

    private static final Logger log = LoggerFactory.getLogger(StoreConfigChangedHandler.class);

    @Override
    public void handle(final StoreConfigChangedEvent event) {
        log.info("[StoreConfigChangedHandler] tenantId={} storeId={} configVersion={} eventId={}",
                event.getTenantId(),
                event.getStoreId(),
                event.getConfigVersion(),
                event.getEventId());

        // TODO 在这里注入并调用领域/应用服务，处理门店配置变更后的应用内业务逻辑。
    }
}

