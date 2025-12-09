package com.bluecone.app.inventory.domain.event;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 库存调整事件（Inventory Adjusted Event）。
 *
 * <p>典型触发时机：
 * <ul>
 *     <li>后台手工盘点 / 纠错；</li>
 *     <li>运营活动触发的批量增减库存。</li>
 * </ul>
 * 强调“调整量（delta）”与“调整后可用库存（afterQuantity）”，便于下游补偿与对账。</p>
 */
public class InventoryAdjustedEvent extends DomainEvent {

    public static final String EVENT_TYPE = "INVENTORY_ADJUSTED";
    private static final String AGGREGATE_TYPE = "INVENTORY";

    private final Long tenantId;
    private final Long storeId;
    private final Long skuId;

    /** 调整量（可为负数，表示减少） */
    private final Long delta;

    /** 调整后的可用库存数量 */
    private final Long afterQuantity;

    /** 调整来源：MANUAL_ADJUST / STOCK_TAKING 等 */
    private final String source;

    /** 操作人 ID，用于审计 */
    private final Long operatorId;

    public InventoryAdjustedEvent(Long tenantId,
                                  Long storeId,
                                  Long skuId,
                                  Long delta,
                                  Long afterQuantity,
                                  String source,
                                  Long operatorId) {
        this(null,
                null,
                null,
                null,
                tenantId,
                storeId,
                skuId,
                delta,
                afterQuantity,
                source,
                operatorId);
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public InventoryAdjustedEvent(@JsonProperty("eventId") String eventId,
                                  @JsonProperty("occurredAt") Instant occurredAt,
                                  @JsonProperty("eventType") String eventType,
                                  @JsonProperty("metadata") EventMetadata metadata,
                                  @JsonProperty("tenantId") Long tenantId,
                                  @JsonProperty("storeId") Long storeId,
                                  @JsonProperty("skuId") Long skuId,
                                  @JsonProperty("delta") Long delta,
                                  @JsonProperty("afterQuantity") Long afterQuantity,
                                  @JsonProperty("source") String source,
                                  @JsonProperty("operatorId") Long operatorId) {
        super(eventId,
                occurredAt,
                eventType == null ? EVENT_TYPE : eventType,
                metadata == null ? buildMetadata(tenantId, storeId, skuId) : metadata);
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.skuId = skuId;
        this.delta = delta;
        this.afterQuantity = afterQuantity;
        this.source = source;
        this.operatorId = operatorId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public Long getSkuId() {
        return skuId;
    }

    public Long getDelta() {
        return delta;
    }

    public Long getAfterQuantity() {
        return afterQuantity;
    }

    public String getSource() {
        return source;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    private static EventMetadata buildMetadata(Long tenantId,
                                               Long storeId,
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
        return attributes.isEmpty() ? EventMetadata.empty() : EventMetadata.of(attributes);
    }
}
