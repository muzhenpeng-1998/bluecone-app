package com.bluecone.app.payment.event;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付成功领域事件：由支付域发出，订单域等下游通过 Outbox 消费。
 */
public class PaymentSucceededEvent extends DomainEvent {

    public static final String EVENT_TYPE = "payment.order.succeeded";

    private final Long tenantId;
    private final Long storeId;
    private final Long userId;
    private final Long orderId;
    private final Long paymentOrderId;
    private final String payChannel;
    private final BigDecimal payAmount;
    private final String currency;
    private final LocalDateTime paidAt;
    private final String channelTradeNo;

    public PaymentSucceededEvent(final Long tenantId,
                                 final Long storeId,
                                 final Long userId,
                                 final Long orderId,
                                 final Long paymentOrderId,
                                 final String payChannel,
                                 final BigDecimal payAmount,
                                 final String currency,
                                 final LocalDateTime paidAt,
                                 final String channelTradeNo,
                                 final String traceId) {
        this(null,
                null,
                null,
                null,
                tenantId,
                storeId,
                userId,
                orderId,
                paymentOrderId,
                payChannel,
                payAmount,
                currency,
                paidAt,
                channelTradeNo,
                traceId);
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public PaymentSucceededEvent(@JsonProperty("eventId") final String eventId,
                                 @JsonProperty("occurredAt") final Instant occurredAt,
                                 @JsonProperty("eventType") final String eventType,
                                 @JsonProperty("metadata") final EventMetadata metadata,
                                 @JsonProperty("tenantId") final Long tenantId,
                                 @JsonProperty("storeId") final Long storeId,
                                 @JsonProperty("userId") final Long userId,
                                 @JsonProperty("orderId") final Long orderId,
                                 @JsonProperty("paymentOrderId") final Long paymentOrderId,
                                 @JsonProperty("payChannel") final String payChannel,
                                 @JsonProperty("payAmount") final BigDecimal payAmount,
                                 @JsonProperty("currency") final String currency,
                                 @JsonProperty("paidAt") final LocalDateTime paidAt,
                                 @JsonProperty("channelTradeNo") final String channelTradeNo,
                                 @JsonProperty("traceId") final String traceId) {
        super(eventId,
                occurredAt,
                eventType == null ? EVENT_TYPE : eventType,
                metadata == null ? buildMetadata(tenantId, storeId, orderId, paymentOrderId, traceId) : metadata);
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.userId = userId;
        this.orderId = orderId;
        this.paymentOrderId = paymentOrderId;
        this.payChannel = payChannel;
        this.payAmount = payAmount;
        this.currency = currency;
        this.paidAt = paidAt;
        this.channelTradeNo = channelTradeNo;
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

    /**
     * 业务订单 ID。
     */
    public Long getOrderId() {
        return orderId;
    }

    public Long getPaymentOrderId() {
        return paymentOrderId;
    }

    public String getPayChannel() {
        return payChannel;
    }

    public BigDecimal getPayAmount() {
        return payAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public String getChannelTradeNo() {
        return channelTradeNo;
    }

    private static EventMetadata buildMetadata(final Long tenantId,
                                               final Long storeId,
                                               final Long orderId,
                                               final Long paymentOrderId,
                                               final String traceId) {
        Map<String, String> attributes = new HashMap<>();
        if (traceId != null && !traceId.isBlank()) {
            attributes.put("traceId", traceId);
        }
        if (tenantId != null) {
            attributes.put("tenantId", String.valueOf(tenantId));
        }
        if (storeId != null) {
            attributes.put("storeId", String.valueOf(storeId));
        }
        if (orderId != null) {
            attributes.put("orderId", String.valueOf(orderId));
        }
        if (paymentOrderId != null) {
            attributes.put("paymentOrderId", String.valueOf(paymentOrderId));
        }
        Long keySource = orderId != null ? orderId : paymentOrderId;
        if (keySource != null) {
            attributes.put("eventKey", EVENT_TYPE + ":" + keySource);
        }
        return attributes.isEmpty() ? EventMetadata.empty() : EventMetadata.of(attributes);
    }

    @Override
    public String toString() {
        return "PaymentSucceededEvent{" +
                "tenantId=" + tenantId +
                ", storeId=" + storeId +
                ", userId=" + userId +
                ", orderId=" + orderId +
                ", paymentOrderId=" + paymentOrderId +
                ", payChannel='" + payChannel + '\'' +
                ", payAmount=" + payAmount +
                ", currency='" + currency + '\'' +
                ", paidAt=" + paidAt +
                ", channelTradeNo='" + channelTradeNo + '\'' +
                ", eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                ", eventType='" + getEventType() + '\'' +
                '}';
    }
}
