package com.bluecone.app.store.event;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 门店配置变更事件。
 */
public class StoreConfigChangedEvent extends DomainEvent {

    private final Long tenantId;
    private final Long storeId;
    private final Long configVersion;

    public StoreConfigChangedEvent(Long tenantId, Long storeId, Long configVersion, EventMetadata metadata) {
        super(StoreEventNames.STORE_CONFIG_CHANGED, metadata);
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.configVersion = configVersion;
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public StoreConfigChangedEvent(@JsonProperty("tenantId") Long tenantId,
                                   @JsonProperty("storeId") Long storeId,
                                   @JsonProperty("configVersion") Long configVersion,
                                   @JsonProperty("eventId") String eventId,
                                   @JsonProperty("occurredAt") java.time.Instant occurredAt,
                                   @JsonProperty("eventType") String eventType,
                                   @JsonProperty("metadata") EventMetadata metadata) {
        super(eventId, occurredAt,
                eventType == null ? StoreEventNames.STORE_CONFIG_CHANGED : eventType,
                metadata);
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.configVersion = configVersion;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public Long getConfigVersion() {
        return configVersion;
    }

    @Override
    public String toString() {
        return "StoreConfigChangedEvent{" +
                "tenantId=" + tenantId +
                ", storeId=" + storeId +
                ", configVersion=" + configVersion +
                ", eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
