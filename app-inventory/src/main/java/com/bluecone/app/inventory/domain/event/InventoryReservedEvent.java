package com.bluecone.app.inventory.domain.event;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 库存预留事件（Inventory Reserved Event）。
 *
 * <p>典型触发时机：订单创建或占用库存成功后，用于通知下游已经锁定指定 SKU 的可用库存。</p>
 *
 * <p>设计约定：
 * <ul>
 *     <li>{@link #AGGREGATE_TYPE} 固定为 {@code INVENTORY}，表示库存域聚合。</li>
 *     <li>{@link #skuId} 作为 {@code aggregateId}，便于按 SKU 维度追踪事件。</li>
 *     <li>{@link #EVENT_TYPE} 固定为 {@code INVENTORY_RESERVED}，供下游路由和 webhook 使用。</li>
 * </ul>
 * </p>
 */
public class InventoryReservedEvent extends DomainEvent {

    public static final String EVENT_TYPE = "INVENTORY_RESERVED";
    private static final String AGGREGATE_TYPE = "INVENTORY";

    /** 租户 ID，用于多租户隔离 */
    private final Long tenantId;

    /** 门店 ID，支持门店级别的库存管控 */
    private final Long storeId;

    /** 关联订单 ID，方便串联订单业务链路 */
    private final Long orderId;

    /** SKU ID，唯一标识具体库存单位 */
    private final Long skuId;

    /** 本次预留的数量（锁定件数） */
    private final Long quantity;

    /** 预留原因，例如：ORDER_PLACE / MANUAL_RESERVE 等 */
    private final String reason;

    public InventoryReservedEvent(Long tenantId,
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
    public InventoryReservedEvent(@JsonProperty("eventId") String eventId,
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
