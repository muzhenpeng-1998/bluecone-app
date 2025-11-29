// File: app-order/src/main/java/com/bluecone/app/order/handler/OutboxDemoOrderPaidHandler.java
package com.bluecone.app.order.handler;

import com.bluecone.app.core.event.EventHandler;
import com.bluecone.app.core.event.annotations.EventHandlerComponent;
import com.bluecone.app.order.event.OrderPaidEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Outbox 投递示例 handler，展示 traceId/tenantId 透传与全链路日志。
 */
@EventHandlerComponent
public class OutboxDemoOrderPaidHandler implements EventHandler<OrderPaidEvent> {

    private static final Logger log = LoggerFactory.getLogger(OutboxDemoOrderPaidHandler.class);

    @Override
    public void handle(final OrderPaidEvent event) {
        String traceId = event.getMetadata().get("traceId");
        String tenantId = event.getMetadata().get("tenantId");
        log.info("[OutboxDemoHandler] orderId={} traceId={} tenantId={} amount={}",
                event.getOrderId(),
                traceId,
                tenantId,
                event.getPayAmount());
    }
}
