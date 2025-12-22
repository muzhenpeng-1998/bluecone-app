// File: app-application/src/main/java/com/bluecone/app/controller/OutboxDemoController.java
package com.bluecone.app.controller;

import com.bluecone.app.core.event.EventMetadata;
import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.order.event.OrderPaidEvent;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Outbox 演示 Controller：模拟发布订单支付事件。
 */
@Tag(name = "🛠️ 开发调试 > 其他调试接口", description = "Outbox模式测试接口")
@RestController
@RequestMapping("/api/outbox")
public class OutboxDemoController {

    private static final Logger log = LoggerFactory.getLogger(OutboxDemoController.class);

    private final DomainEventPublisher publisher;

    public OutboxDemoController(final DomainEventPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/test")
    public String fireOrderPaid(@RequestParam("orderId") Long orderId,
                                @RequestParam(value = "tenantId", required = false) Long tenantId,
                                @RequestParam(value = "userId", required = false) Long userId,
                                @RequestParam(value = "amount", required = false) BigDecimal amount,
                                @RequestParam(value = "channel", required = false) String channel) {
        Map<String, String> meta = new HashMap<>();
        String traceId = MDC.get("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            MDC.put("traceId", traceId);
        }
        meta.put("traceId", traceId);
        if (tenantId != null) {
            meta.put("tenantId", tenantId.toString());
        }
        if (userId != null) {
            meta.put("userId", userId.toString());
        }
        EventMetadata metadata = EventMetadata.of(meta);
        OrderPaidEvent event = new OrderPaidEvent(
                orderId,
                tenantId,
                userId,
                amount != null ? amount : BigDecimal.TEN,
                channel != null ? channel : "DEMO",
                metadata
        );
        publisher.publish(event);
        log.info("[OutboxDemo] submitted orderPaid event orderId={} traceId={}", orderId, traceId);
        return "queued";
    }
}
