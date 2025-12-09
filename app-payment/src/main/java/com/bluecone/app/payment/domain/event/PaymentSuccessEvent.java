package com.bluecone.app.payment.domain.event;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 由支付域在支付成功后发出的领域事件。
 */
public class PaymentSuccessEvent extends DomainEvent {

    public static final String EVENT_TYPE = "PAYMENT_SUCCESS";
    private static final String AGGREGATE_TYPE = "PAYMENT";

    private final Long tenantId;
    private final Long storeId;
    private final Long userId;
    private final Long orderId;
    private final Long payOrderId;
    private final Long totalAmount;
    private final Long paidAmount;
    private final String channel;
    private final String outTransactionNo;

    public PaymentSuccessEvent(Long tenantId,
                               Long storeId,
                               Long userId,
                               Long orderId,
                               Long payOrderId,
                               Long totalAmount,
                               Long paidAmount,
                               String channel,
                               String outTransactionNo) {
        super(EVENT_TYPE, buildMetadata(tenantId, storeId, userId, orderId, payOrderId));
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.userId = userId;
        this.orderId = orderId;
        this.payOrderId = payOrderId;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.channel = channel;
        this.outTransactionNo = outTransactionNo;
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public PaymentSuccessEvent(@JsonProperty("eventId") String eventId,
                               @JsonProperty("occurredAt") Instant occurredAt,
                               @JsonProperty("eventType") String eventType,
                               @JsonProperty("metadata") EventMetadata metadata,
                               @JsonProperty("tenantId") Long tenantId,
                               @JsonProperty("storeId") Long storeId,
                               @JsonProperty("userId") Long userId,
                               @JsonProperty("orderId") Long orderId,
                               @JsonProperty("payOrderId") Long payOrderId,
                               @JsonProperty("totalAmount") Long totalAmount,
                               @JsonProperty("paidAmount") Long paidAmount,
                               @JsonProperty("channel") String channel,
                               @JsonProperty("outTransactionNo") String outTransactionNo) {
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
        this.paidAmount = paidAmount;
        this.channel = channel;
        this.outTransactionNo = outTransactionNo;
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

    public Long getPaidAmount() {
        return paidAmount;
    }

    public String getChannel() {
        return channel;
    }

    public String getOutTransactionNo() {
        return outTransactionNo;
    }

    private static EventMetadata buildMetadata(Long tenantId,
                                               Long storeId,
                                               Long userId,
                                               Long orderId,
                                               Long payOrderId) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("aggregateType", AGGREGATE_TYPE);
        if (payOrderId != null) {
            attributes.put("aggregateId", String.valueOf(payOrderId));
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
        if (orderId != null) {
            attributes.put("orderId", String.valueOf(orderId));
        }
        if (payOrderId != null) {
            attributes.put("payOrderId", String.valueOf(payOrderId));
        }
        return attributes.isEmpty() ? EventMetadata.empty() : EventMetadata.of(attributes);
    }
}
