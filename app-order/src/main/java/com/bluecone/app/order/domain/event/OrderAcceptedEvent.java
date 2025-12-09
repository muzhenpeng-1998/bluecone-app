package com.bluecone.app.order.domain.event;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 商户接单成功后的领域事件。
 */
public class OrderAcceptedEvent extends DomainEvent {

    public static final String EVENT_TYPE = "ORDER_ACCEPTED";
    private static final String AGGREGATE_TYPE = "ORDER";

    private final Long tenantId;
    private final Long storeId;
    private final Long orderId;
    private final Long operatorId;
    private final Long payOrderId;
    private final Long totalAmount;

    public OrderAcceptedEvent(Long tenantId,
                              Long storeId,
                              Long orderId,
                              Long operatorId,
                              Long payOrderId,
                              Long totalAmount) {
        super(EVENT_TYPE, buildMetadata(tenantId, storeId, orderId, payOrderId));
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.orderId = orderId;
        this.operatorId = operatorId;
        this.payOrderId = payOrderId;
        this.totalAmount = totalAmount;
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrderAcceptedEvent(@JsonProperty("eventId") String eventId,
                              @JsonProperty("occurredAt") Instant occurredAt,
                              @JsonProperty("eventType") String eventType,
                              @JsonProperty("metadata") EventMetadata metadata,
                              @JsonProperty("tenantId") Long tenantId,
                              @JsonProperty("storeId") Long storeId,
                              @JsonProperty("orderId") Long orderId,
                              @JsonProperty("operatorId") Long operatorId,
                              @JsonProperty("payOrderId") Long payOrderId,
                              @JsonProperty("totalAmount") Long totalAmount) {
        super(eventId,
                occurredAt,
                eventType == null ? EVENT_TYPE : eventType,
                metadata == null ? buildMetadata(tenantId, storeId, orderId, payOrderId) : metadata);
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.orderId = orderId;
        this.operatorId = operatorId;
        this.payOrderId = payOrderId;
        this.totalAmount = totalAmount;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public Long getPayOrderId() {
        return payOrderId;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    private static EventMetadata buildMetadata(Long tenantId,
                                               Long storeId,
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
        if (payOrderId != null) {
            attributes.put("payOrderId", String.valueOf(payOrderId));
        }
        return attributes.isEmpty() ? EventMetadata.empty() : EventMetadata.of(attributes);
    }
}
