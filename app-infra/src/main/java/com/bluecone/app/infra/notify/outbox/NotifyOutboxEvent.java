package com.bluecone.app.infra.notify.outbox;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import com.bluecone.app.infra.notify.model.NotificationIntent;
import com.bluecone.app.infra.notify.model.NotificationTask;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Outbox 事件载荷（Outbox 层）。
 */
public class NotifyOutboxEvent extends DomainEvent {

    private final NotificationIntent intent;
    private final NotificationTask task;

    public NotifyOutboxEvent(NotificationIntent intent, NotificationTask task) {
        this(null, null, "notification.dispatch", buildMetadata(intent), intent, task);
    }

    @JsonCreator
    public NotifyOutboxEvent(@JsonProperty("eventId") String eventId,
                             @JsonProperty("occurredAt") Instant occurredAt,
                             @JsonProperty("eventType") String eventType,
                             @JsonProperty("metadata") EventMetadata metadata,
                             @JsonProperty("intent") NotificationIntent intent,
                             @JsonProperty("task") NotificationTask task) {
        super(eventId, occurredAt, eventType, metadata);
        this.intent = Objects.requireNonNull(intent, "intent must not be null");
        this.task = Objects.requireNonNull(task, "task must not be null");
    }

    public NotificationIntent getIntent() {
        return intent;
    }

    public NotificationTask getTask() {
        return task;
    }

    private static EventMetadata buildMetadata(NotificationIntent intent) {
        Map<String, String> attributes = new HashMap<>();
        if (intent.getTraceId() != null) {
            attributes.put("traceId", intent.getTraceId());
        }
        if (intent.getTenantId() != null) {
            attributes.put("tenantId", String.valueOf(intent.getTenantId()));
        }
        attributes.put("eventKey", intent.getScenarioCode() + ":" + intent.getIdempotentKey());
        return EventMetadata.of(attributes);
    }
}
