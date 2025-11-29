// File: app-order/src/main/java/com/bluecone/app/order/event/OrderPaidEvent.java
package com.bluecone.app.order.event;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 代表订单支付成功的标准领域事件。
 *
 * <p>典型下游：库存扣减、打印、派单、用户/店主通知、账务、营销触达等。</p>
 */
public class OrderPaidEvent extends DomainEvent {

    private final Long orderId;
    private final Long tenantId;
    private final Long userId;
    private final BigDecimal payAmount;
    private final String payChannel;

    public OrderPaidEvent(final Long orderId,
                          final Long tenantId,
                          final Long userId,
                          final BigDecimal payAmount,
                          final String payChannel,
                          final EventMetadata metadata) {
        super(OrderEventNames.ORDER_PAID, metadata);
        this.orderId = orderId;
        this.tenantId = tenantId;
        this.userId = userId;
        this.payAmount = payAmount;
        this.payChannel = payChannel;
    }

    /**
     * 用于 Jackson 反序列化 Outbox 中的事件。
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrderPaidEvent(@JsonProperty("orderId") final Long orderId,
                          @JsonProperty("tenantId") final Long tenantId,
                          @JsonProperty("userId") final Long userId,
                          @JsonProperty("payAmount") final BigDecimal payAmount,
                          @JsonProperty("payChannel") final String payChannel,
                          @JsonProperty("eventId") final String eventId,
                          @JsonProperty("occurredAt") final java.time.Instant occurredAt,
                          @JsonProperty("eventType") final String eventType,
                          @JsonProperty("metadata") final EventMetadata metadata) {
        super(eventId, occurredAt, eventType == null ? OrderEventNames.ORDER_PAID : eventType, metadata);
        this.orderId = orderId;
        this.tenantId = tenantId;
        this.userId = userId;
        this.payAmount = payAmount;
        this.payChannel = payChannel;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getPayAmount() {
        return payAmount;
    }

    public String getPayChannel() {
        return payChannel;
    }

    @Override
    public String toString() {
        return "OrderPaidEvent{" +
                "orderId=" + orderId +
                ", tenantId=" + tenantId +
                ", userId=" + userId +
                ", payAmount=" + payAmount +
                ", payChannel='" + payChannel + '\'' +
                ", eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                ", eventType='" + getEventType() + '\'' +
                '}';
    }
}
