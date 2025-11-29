// File: app-order/src/main/java/com/bluecone/app/order/event/OrderEventNames.java
package com.bluecone.app.order.event;

/**
 * 订单领域事件名集中管理。
 *
 * <p>统一维护语义名，方便扩展（order.created / order.cancelled / order.refunded 等），避免散落硬编码。</p>
 */
public final class OrderEventNames {

    private OrderEventNames() {
    }

    public static final String ORDER_PAID = "order.paid";
}
