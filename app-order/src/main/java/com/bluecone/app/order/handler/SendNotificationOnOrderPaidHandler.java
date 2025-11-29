// File: app-order/src/main/java/com/bluecone/app/order/handler/SendNotificationOnOrderPaidHandler.java
package com.bluecone.app.order.handler;

import com.bluecone.app.core.event.EventHandler;
import com.bluecone.app.core.event.annotations.EventHandlerComponent;
import com.bluecone.app.order.event.OrderPaidEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 示例 handler：订单支付后通知用户/店主。
 *
 * <p>未来可对接 NotificationService，发送短信/小程序模版/公众号/APP 推送等。</p>
 */
@EventHandlerComponent
public class SendNotificationOnOrderPaidHandler implements EventHandler<OrderPaidEvent> {

    private static final Logger log = LoggerFactory.getLogger(SendNotificationOnOrderPaidHandler.class);

    @Override
    public void handle(final OrderPaidEvent event) {
        log.info("[OrderPaid->Notify] orderId={} tenantId={} userId={} amount={} channel={}",
                event.getOrderId(),
                event.getTenantId(),
                event.getUserId(),
                event.getPayAmount(),
                event.getPayChannel());
        // TODO integrate NotificationService for SMS/mini-program/official account pushes.
    }
}
