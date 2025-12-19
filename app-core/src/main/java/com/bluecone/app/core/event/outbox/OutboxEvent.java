package com.bluecone.app.core.event.outbox;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outbox 事件模型
 * 用于在业务事务中写入 Outbox 表，保证事件与业务数据的一致性
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    
    /**
     * 租户ID（多租户隔离）
     */
    private Long tenantId;
    
    /**
     * 门店ID（业务隔离）
     */
    private Long storeId;
    
    /**
     * 聚合根类型（ORDER/PAYMENT/REFUND 等）
     */
    private AggregateType aggregateType;
    
    /**
     * 聚合根ID（订单ID、支付单ID等）
     */
    private String aggregateId;
    
    /**
     * 事件类型（order.checkout_locked/order.paid 等）
     */
    private EventType eventType;
    
    /**
     * 事件唯一ID（UUID/ULID）
     */
    private String eventId;
    
    /**
     * 事件载荷（业务数据）
     */
    private Map<String, Object> payload;
    
    /**
     * 事件元数据（traceId/requestId/userId 等）
     */
    private Map<String, Object> metadata;
    
    /**
     * 下次重试时间（用于延迟投递）
     */
    private LocalDateTime nextRetryAt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 便捷方法：创建订单事件
     */
    public static OutboxEvent forOrder(Long tenantId, Long storeId, Long orderId, EventType eventType, 
                                       Map<String, Object> payload, Map<String, Object> metadata) {
        return OutboxEvent.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .aggregateType(AggregateType.ORDER)
                .aggregateId(String.valueOf(orderId))
                .eventType(eventType)
                .eventId(java.util.UUID.randomUUID().toString())
                .payload(payload)
                .metadata(metadata)
                .nextRetryAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 便捷方法：创建支付事件
     */
    public static OutboxEvent forPayment(Long tenantId, Long storeId, Long paymentId, EventType eventType,
                                         Map<String, Object> payload, Map<String, Object> metadata) {
        return OutboxEvent.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .aggregateType(AggregateType.PAYMENT)
                .aggregateId(String.valueOf(paymentId))
                .eventType(eventType)
                .eventId(java.util.UUID.randomUUID().toString())
                .payload(payload)
                .metadata(metadata)
                .nextRetryAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 便捷方法：创建退款事件
     */
    public static OutboxEvent forRefund(Long tenantId, Long storeId, Long refundId, EventType eventType,
                                        Map<String, Object> payload, Map<String, Object> metadata) {
        return OutboxEvent.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .aggregateType(AggregateType.REFUND)
                .aggregateId(String.valueOf(refundId))
                .eventType(eventType)
                .eventId(java.util.UUID.randomUUID().toString())
                .payload(payload)
                .metadata(metadata)
                .nextRetryAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
