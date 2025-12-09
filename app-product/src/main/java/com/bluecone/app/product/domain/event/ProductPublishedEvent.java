package com.bluecone.app.product.domain.event;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 商品发布/上架事件（Product Published Event）。
 *
 * <p>触发场景：当某个 SKU 从“草稿/下架”切换为“可售”状态时，通知下游实时同步新价格与可售状态。</p>
 * <p>设计要点：
 * <ul>
 *     <li>aggregateType 固定为 PRODUCT，aggregateId 使用 skuId，方便订阅方按 SKU 粒度处理。</li>
 *     <li>事件体携带 tenant/store/spu/sku/价格/上架状态等关键信息，便于搜索、推荐、缓存系统直接消费。</li>
 * </ul>
 * </p>
 */
public class ProductPublishedEvent extends DomainEvent {

    public static final String EVENT_TYPE = "PRODUCT_PUBLISHED";
    private static final String AGGREGATE_TYPE = "PRODUCT";

    private final Long tenantId;
    private final Long storeId;
    private final Long spuId;
    private final Long skuId;
    private final String productName;
    private final Long salePrice;
    private final Boolean enabled;

    public ProductPublishedEvent(Long tenantId,
                                 Long storeId,
                                 Long spuId,
                                 Long skuId,
                                 String productName,
                                 Long salePrice,
                                 Boolean enabled) {
        this(null,
                null,
                null,
                null,
                tenantId,
                storeId,
                spuId,
                skuId,
                productName,
                salePrice,
                enabled);
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ProductPublishedEvent(@JsonProperty("eventId") String eventId,
                                 @JsonProperty("occurredAt") Instant occurredAt,
                                 @JsonProperty("eventType") String eventType,
                                 @JsonProperty("metadata") EventMetadata metadata,
                                 @JsonProperty("tenantId") Long tenantId,
                                 @JsonProperty("storeId") Long storeId,
                                 @JsonProperty("spuId") Long spuId,
                                 @JsonProperty("skuId") Long skuId,
                                 @JsonProperty("productName") String productName,
                                 @JsonProperty("salePrice") Long salePrice,
                                 @JsonProperty("enabled") Boolean enabled) {
        super(eventId,
                occurredAt,
                eventType == null ? EVENT_TYPE : eventType,
                metadata == null ? buildMetadata(tenantId, storeId, skuId, spuId) : metadata);
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.spuId = spuId;
        this.skuId = skuId;
        this.productName = productName;
        this.salePrice = salePrice;
        this.enabled = enabled;
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

    public String getProductName() {
        return productName;
    }

    public Long getSalePrice() {
        return salePrice;
    }

    public Boolean getEnabled() {
        return enabled;
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
