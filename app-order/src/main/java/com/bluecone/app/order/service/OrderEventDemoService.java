// File: app-order/src/main/java/com/bluecone/app/order/service/OrderEventDemoService.java
package com.bluecone.app.order.service;

import com.bluecone.app.core.event.EventMetadata;
import com.bluecone.app.core.event.EventOrchestrator;
import com.bluecone.app.order.event.OrderPaidEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 演示在核心业务动作后发布领域事件的服务。
 *
 * <p>事件应在服务层（而非控制器）发布，确保代表已成功的领域事实；订单更新失败则不应发布，
 * 一旦发布也不应回滚。</p>
 */
@Service
public class OrderEventDemoService {

    private static final Logger log = LoggerFactory.getLogger(OrderEventDemoService.class);

    private final EventOrchestrator eventOrchestrator;

    public OrderEventDemoService(final EventOrchestrator eventOrchestrator) {
        this.eventOrchestrator = eventOrchestrator;
    }

    public void markOrderPaid(final Long orderId,
                              final Long tenantId,
                              final Long userId,
                              final BigDecimal amount,
                              final String payChannel) {
        log.info("Marking order as paid orderId={} tenantId={} userId={} amount={} channel={}",
                orderId, tenantId, userId, amount, payChannel);
        // TODO update order state to PAID with proper transaction boundaries.

        Map<String, String> meta = new HashMap<>();
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            meta.put("traceId", traceId);
        }
        if (tenantId != null) {
            meta.put("tenantId", tenantId.toString());
        }
        if (userId != null) {
            meta.put("userId", userId.toString());
        }
        EventMetadata metadata = meta.isEmpty() ? EventMetadata.empty() : EventMetadata.of(meta);

        OrderPaidEvent event = new OrderPaidEvent(
                orderId,
                tenantId,
                userId,
                amount,
                payChannel,
                metadata
        );

        eventOrchestrator.fire(event);
    }
}
