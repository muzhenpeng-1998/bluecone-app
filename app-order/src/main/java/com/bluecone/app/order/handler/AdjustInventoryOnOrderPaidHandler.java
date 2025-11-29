// File: app-order/src/main/java/com/bluecone/app/order/handler/AdjustInventoryOnOrderPaidHandler.java
package com.bluecone.app.order.handler;

import com.bluecone.app.core.event.EventHandler;
import com.bluecone.app.core.event.annotations.EventHandlerComponent;
import com.bluecone.app.order.event.OrderPaidEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 示例 handler：订单支付后调整库存。
 *
 * <p>未来可注入 InventoryService 完成扣减、补偿，并发布后续事件。</p>
 */
@EventHandlerComponent
public class AdjustInventoryOnOrderPaidHandler implements EventHandler<OrderPaidEvent> {

    private static final Logger log = LoggerFactory.getLogger(AdjustInventoryOnOrderPaidHandler.class);

    @Override
    public void handle(final OrderPaidEvent event) {
        log.info("[OrderPaid->Inventory] orderId={} tenantId={} userId={} amount={} channel={}",
                event.getOrderId(),
                event.getTenantId(),
                event.getUserId(),
                event.getPayAmount(),
                event.getPayChannel());
        // TODO inject InventoryService to perform stock deduction and emit follow-up events.
    }
}
