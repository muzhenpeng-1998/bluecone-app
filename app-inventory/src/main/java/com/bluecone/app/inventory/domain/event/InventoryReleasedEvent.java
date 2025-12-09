package com.bluecone.app.inventory.domain.event;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 库存释放事件（Inventory Released Event）。
 *
 * <p>典型触发时机：订单取消、支付失败或锁定超时，需要将之前锁定的库存释放回可用池。</p>
 */
public class InventoryReleasedEvent extends DomainEvent {

    public static final String EVENT_TYPE = "INVENTORY_RELEASED";
    private static final String AGGREGATE_TYPE = "INVENTORY";

    private final Long tenantId;
    private final Long storeId;
    private final Long orderId;
    private final Long skuId;
    private final Long quantity;

    /** 释放原因：ORDER_CANCEL / PAYMENT_TIMEOUT / RESERVE_FAIL 等 */
    private final String reason;

    public InventoryReleasedEvent(Long tenantId,
                                  Long storeId,
                                  Long orderId,
                                  Long skuId,
                                  Long quantity,
                                  String reason) {
        this(null,
                null,
                null,
                null,
                tenantId,
                storeId,
                orderId,
                skuId,
                quantity,
                reason);
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public InventoryReleasedEvent(@JsonProperty("eventId") String eventId,
                                  @JsonProperty("occurredAt") Instant occurredAt,
                                  @JsonProperty("eventType") String eventType,
                                  @JsonProperty("metadata") EventMetadata metadata,
                                  @JsonProperty("tenantId") Long tenantId,
                                  @JsonProperty("storeId") Long storeId,
                                  @JsonProperty("orderId") Long orderId,
                                  @JsonProperty("skuId") Long skuId,
                                  @JsonProperty("quantity") Long quantity,
                                  @JsonProperty("reason") String reason) {
        super(eventId,
                occurredAt,
                eventType == null ? EVENT_TYPE : eventType,
                metadata == null ? buildMetadata(tenantId, storeId, orderId, skuId) : metadata);
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.orderId = orderId;
        this.skuId = skuId;
        this.quantity = quantity;
        this.reason = reason;
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

    public Long getSkuId() {
        return skuId;
    }

    public Long getQuantity() {
        return quantity;
    }

    public String getReason() {
        return reason;
    }

    private static EventMetadata buildMetadata(Long tenantId,
                                               Long storeId,
                                               Long orderId,
                                               Long skuId) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("aggregateType", AGGREGATE_TYPE);
        if (skuId != null) {
            attributes.put("aggregateId", String.valueOf(skuId));
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
        return attributes.isEmpty() ? EventMetadata.empty() : EventMetadata.of(attributes);
    }
}
