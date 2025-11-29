// File: app-core/src/main/java/com/bluecone/app/core/event/EventMetadata.java
package com.bluecone.app.core.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * 事件级元数据的不可变容器。
 *
 * <p>常用于承载 traceId / spanId / tenantId / userId / sourceApp / deviceId 等上下文。
 * 建议与日志 MDC 对齐，保证日志、指标、下游 sink 共享相同的关联标识。</p>
 */
public final class EventMetadata {

    private final Map<String, String> attributes;

    @JsonCreator
    private EventMetadata(@JsonProperty("attributes") final Map<String, String> attributes) {
        this.attributes = attributes == null ? Collections.emptyMap() : attributes;
    }

    /**
     * @return 无任何属性的空元数据
     */
    public static EventMetadata empty() {
        return new EventMetadata(Collections.emptyMap());
    }

    /**
     * 从外部属性构造元数据。
     *
     * @param attributes 键值对（会防御性复制）
     * @return 不可变元数据
     */
    public static EventMetadata of(final Map<String, String> attributes) {
        Objects.requireNonNull(attributes, "attributes must not be null");
        return new EventMetadata(Map.copyOf(attributes));
    }

    /**
     * @return 所有属性的不可变视图
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    /**
     * 获取单个属性值。
     *
     * @param key 属性名
     * @return 属性值，不存在返回 null
     */
    public String get(final String key) {
        return attributes.get(key);
    }

    @Override
    public String toString() {
        return "EventMetadata{" +
                "attributes=" + attributes +
                '}';
    }
}
