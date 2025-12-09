package com.bluecone.app.order.domain.event;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单提交后的领域事件，写入 Outbox 供异步消费。
 */
public class OrderSubmittedEvent extends DomainEvent {

    public static final String EVENT_TYPE = "ORDER_SUBMITTED";
    private static final String AGGREGATE_TYPE = "ORDER";

    private final Long tenantId;
    private final Long storeId;
    private final Long userId;
    private final Long orderId;
    private final Long payOrderId;
    private final Long totalAmount;
    private final String channel;

    public OrderSubmittedEvent(Long tenantId,
                               Long storeId,
                               Long userId,
                               Long orderId,
                               Long payOrderId,
                               Long totalAmount,
                               String channel) {
        super(EVENT_TYPE, buildMetadata(tenantId, storeId, userId, orderId, payOrderId));
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.userId = userId;
        this.orderId = orderId;
        this.payOrderId = payOrderId;
        this.totalAmount = totalAmount;
        this.channel = channel;
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrderSubmittedEvent(@JsonProperty("eventId") String eventId,
                               @JsonProperty("occurredAt") Instant occurredAt,
                               @JsonProperty("eventType") String eventType,
                               @JsonProperty("metadata") EventMetadata metadata,
                               @JsonProperty("tenantId") Long tenantId,
                               @JsonProperty("storeId") Long storeId,
                               @JsonProperty("userId") Long userId,
                               @JsonProperty("orderId") Long orderId,
                               @JsonProperty("payOrderId") Long payOrderId,
                               @JsonProperty("totalAmount") Long totalAmount,
                               @JsonProperty("channel") String channel) {
        super(eventId,
                occurredAt,
                eventType == null ? EVENT_TYPE : eventType,
                metadata == null ? buildMetadata(tenantId, storeId, userId, orderId, payOrderId) : metadata);
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.userId = userId;
        this.orderId = orderId;
        this.payOrderId = payOrderId;
        this.totalAmount = totalAmount;
        this.channel = channel;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getPayOrderId() {
        return payOrderId;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public String getChannel() {
        return channel;
    }

    private static EventMetadata buildMetadata(Long tenantId,
                                               Long storeId,
                                               Long userId,
                                               Long orderId,
                                               Long payOrderId) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("aggregateType", AGGREGATE_TYPE);
        if (orderId != null) {
            attributes.put("aggregateId", String.valueOf(orderId));
        }
        if (tenantId != null) {
            attributes.put("tenantId", String.valueOf(tenantId));
        }
        if (storeId != null) {
            attributes.put("storeId", String.valueOf(storeId));
        }
        if (userId != null) {
            attributes.put("userId", String.valueOf(userId));
        }
        if (payOrderId != null) {
            attributes.put("payOrderId", String.valueOf(payOrderId));
        }
        return attributes.isEmpty() ? EventMetadata.empty() : EventMetadata.of(attributes);
    }
}
