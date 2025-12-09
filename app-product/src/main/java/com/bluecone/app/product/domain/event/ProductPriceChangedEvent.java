package com.bluecone.app.product.domain.event;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 商品价格变更事件（Product Price Changed Event）。
 *
 * <p>场景示例：商户/运营后台调整 SKU 售价、活动价回调等。
 * 事件中同时包含 oldPrice/newPrice，便于下游对账或计算差异。</p>
 */
public class ProductPriceChangedEvent extends DomainEvent {

    public static final String EVENT_TYPE = "PRODUCT_PRICE_CHANGED";
    private static final String AGGREGATE_TYPE = "PRODUCT";

    private final Long tenantId;
    private final Long storeId;
    private final Long spuId;
    private final Long skuId;
    private final Long oldPrice;
    private final Long newPrice;
    private final Long operatorId;

    public ProductPriceChangedEvent(Long tenantId,
                                    Long storeId,
                                    Long spuId,
                                    Long skuId,
                                    Long oldPrice,
                                    Long newPrice,
                                    Long operatorId) {
        this(null,
                null,
                null,
                null,
                tenantId,
                storeId,
                spuId,
                skuId,
                oldPrice,
                newPrice,
                operatorId);
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ProductPriceChangedEvent(@JsonProperty("eventId") String eventId,
                                    @JsonProperty("occurredAt") Instant occurredAt,
                                    @JsonProperty("eventType") String eventType,
                                    @JsonProperty("metadata") EventMetadata metadata,
                                    @JsonProperty("tenantId") Long tenantId,
                                    @JsonProperty("storeId") Long storeId,
                                    @JsonProperty("spuId") Long spuId,
                                    @JsonProperty("skuId") Long skuId,
                                    @JsonProperty("oldPrice") Long oldPrice,
                                    @JsonProperty("newPrice") Long newPrice,
                                    @JsonProperty("operatorId") Long operatorId) {
        super(eventId,
                occurredAt,
                eventType == null ? EVENT_TYPE : eventType,
                metadata == null ? buildMetadata(tenantId, storeId, skuId, spuId) : metadata);
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.spuId = spuId;
        this.skuId = skuId;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.operatorId = operatorId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public Long getSpuId() {
        return spuId;
    }

    public Long getSkuId() {
        return skuId;
    }

    public Long getOldPrice() {
        return oldPrice;
    }

    public Long getNewPrice() {
        return newPrice;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    private static EventMetadata buildMetadata(Long tenantId,
                                               Long storeId,
                                               Long skuId,
                                               Long spuId) {
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
        if (spuId != null) {
            attributes.put("spuId", String.valueOf(spuId));
        }
        return EventMetadata.of(attributes);
    }
}
