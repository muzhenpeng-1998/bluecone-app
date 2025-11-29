// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/core/DefaultEventSerializer.java
package com.bluecone.app.infra.outbox.core;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 默认 JSON 序列化器，使用 Jackson 将事件与元数据持久化到 Outbox。
 *
 * <p>头部存储事件类名与事件类型，同时透传 {@link EventMetadata} 的属性，便于路由和观测。</p>
 */
public class DefaultEventSerializer implements EventSerializer {

    public static final String HEADER_EVENT_CLASS = "eventClass";
    public static final String HEADER_EVENT_TYPE = "eventType";

    private static final Logger log = LoggerFactory.getLogger(DefaultEventSerializer.class);

    private final ObjectMapper objectMapper;

    public DefaultEventSerializer(final ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public String serializePayload(final DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload: " + event.getEventType(), e);
        }
    }

    @Override
    public Map<String, String> serializeHeaders(final DomainEvent event) {
        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER_EVENT_CLASS, ClassUtils.getUserClass(event).getName());
        headers.put(HEADER_EVENT_TYPE, event.getEventType());
        EventMetadata metadata = event.getMetadata();
        if (metadata != null) {
            headers.putAll(metadata.getAttributes());
        }
        return headers;
    }

    @Override
    public DomainEvent deserialize(final String payload, final Map<String, String> headers) {
        try {
            String eventClassName = headers.get(HEADER_EVENT_CLASS);
            if (eventClassName == null) {
                throw new IllegalStateException("Missing eventClass header for payload");
            }
            Class<?> clazz = Class.forName(eventClassName);
            Object obj = objectMapper.readValue(payload, clazz);
            if (!(obj instanceof DomainEvent domainEvent)) {
                throw new IllegalStateException("Deserialized object is not a DomainEvent: " + clazz.getName());
            }
            return domainEvent;
        } catch (Exception e) {
            log.error("Failed to deserialize outbox event, headers={}", headers, e);
            throw new IllegalStateException("Failed to deserialize outbox event", e);
        }
    }
}
