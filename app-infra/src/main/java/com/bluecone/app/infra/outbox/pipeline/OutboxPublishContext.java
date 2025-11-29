// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/pipeline/OutboxPublishContext.java
package com.bluecone.app.infra.outbox.pipeline;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.infra.outbox.entity.OutboxMessageEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Outbox 发布链的上下文，贯穿各步骤传递数据。
 */
public class OutboxPublishContext {

    private final DomainEvent event;
    private String payload;
    private Map<String, String> headers = new HashMap<>();
    private OutboxMessageEntity entity;

    public OutboxPublishContext(final DomainEvent event) {
        this.event = Objects.requireNonNull(event, "event must not be null");
    }

    public DomainEvent getEvent() {
        return event;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(final String payload) {
        this.payload = payload;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(final Map<String, String> headers) {
        this.headers = headers;
    }

    public OutboxMessageEntity getEntity() {
        return entity;
    }

    public void setEntity(final OutboxMessageEntity entity) {
        this.entity = entity;
    }
}
