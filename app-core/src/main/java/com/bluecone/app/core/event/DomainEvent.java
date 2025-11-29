// File: app-core/src/main/java/com/bluecone/app/core/event/DomainEvent.java
package com.bluecone.app.core.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 核心层的领域事件基类。
 *
 * <p>事件被设计为不可变值对象，便于在 pipeline / router / handler / sink 之间安全传递，避免意外修改。
 * eventType 使用语义化字符串（如 {@code order.paid}）而非类名，保证跨重构、跨语言或跨系统时含义稳定。</p>
 *
 * <p>{@code eventId} 目前使用 {@link UUID} 生成，未来可统一替换为 ULID/Snowflake 以获得更好的有序性或存储效率；
 * 将生成逻辑集中在基类，便于整体替换。</p>
 */
public abstract class DomainEvent {

    private final String eventId;
    private final Instant occurredAt;
    private final String eventType;
    private final EventMetadata metadata;

    /**
     * 受保护构造函数，由具体事件调用（新事件）。
     *
     * @param eventType 事件语义名（如 "order.paid"，不绑定类名）
     * @param metadata  上下文信息（trace/tenant/user），允许为 null 时使用空元数据
     */
    protected DomainEvent(final String eventType, final EventMetadata metadata) {
        this(null, null, eventType, metadata);
    }

    /**
     * Jackson 反序列化入口，允许从 Outbox 恢复事件（保留 eventId/occurredAt）。
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    protected DomainEvent(@JsonProperty("eventId") final String eventId,
                          @JsonProperty("occurredAt") final Instant occurredAt,
                          @JsonProperty("eventType") final String eventType,
                          @JsonProperty("metadata") final EventMetadata metadata) {
        this.eventId = eventId == null ? UUID.randomUUID().toString() : eventId;
        this.occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.metadata = metadata == null ? EventMetadata.empty() : metadata;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getEventType() {
        return eventType;
    }

    public EventMetadata getMetadata() {
        return metadata;
    }

    /**
     * 便捷获取 traceId（如元数据中存在）。
     */
    public String getTraceId() {
        return metadata.get("traceId");
    }

    /**
     * 便捷获取 tenantId（如元数据中存在且可解析）。
     */
    public Long getTenantId() {
        String raw = metadata.get("tenantId");
        if (raw == null) {
            return null;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "DomainEvent{" +
                "eventId='" + eventId + '\'' +
                ", occurredAt=" + occurredAt +
                ", eventType='" + eventType + '\'' +
                ", metadata=" + metadata +
                '}';
    }
}
