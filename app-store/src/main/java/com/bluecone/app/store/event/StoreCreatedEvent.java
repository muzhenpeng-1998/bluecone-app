package com.bluecone.app.store.event;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import com.bluecone.app.id.core.Ulid128;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 门店创建完成事件。
 *
 * <p>用于向下游同步门店基础信息（tenantId/publicId/storeNo/storeName 等）。</p>
 */
public class StoreCreatedEvent extends DomainEvent {

    private final Ulid128 storeInternalId;
    private final String storePublicId;
    private final Long storeNo;
    private final String storeName;

    public StoreCreatedEvent(Ulid128 storeInternalId,
                             String storePublicId,
                             Long storeNo,
                             String storeName,
                             EventMetadata metadata) {
        super(StoreEventNames.STORE_CREATED, metadata);
        this.storeInternalId = storeInternalId;
        this.storePublicId = storePublicId;
        this.storeNo = storeNo;
        this.storeName = storeName;
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public StoreCreatedEvent(@JsonProperty("storeInternalId") Ulid128 storeInternalId,
                             @JsonProperty("storePublicId") String storePublicId,
                             @JsonProperty("storeNo") Long storeNo,
                             @JsonProperty("storeName") String storeName,
                             @JsonProperty("eventId") String eventId,
                             @JsonProperty("occurredAt") java.time.Instant occurredAt,
                             @JsonProperty("eventType") String eventType,
                             @JsonProperty("metadata") EventMetadata metadata) {
        super(eventId, occurredAt,
                eventType == null ? StoreEventNames.STORE_CREATED : eventType,
                metadata);
        this.storeInternalId = storeInternalId;
        this.storePublicId = storePublicId;
        this.storeNo = storeNo;
        this.storeName = storeName;
    }

    public Ulid128 getStoreInternalId() {
        return storeInternalId;
    }

    public String getStorePublicId() {
        return storePublicId;
    }

    public Long getStoreNo() {
        return storeNo;
    }

    public String getStoreName() {
        return storeName;
    }

    @Override
    public String toString() {
        return "StoreCreatedEvent{" +
                "storeInternalId=" + storeInternalId +
                ", storePublicId='" + storePublicId + '\'' +
                ", storeNo=" + storeNo +
                ", storeName='" + storeName + '\'' +
                ", eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                ", eventType='" + getEventType() + '\'' +
                '}';
    }
}

