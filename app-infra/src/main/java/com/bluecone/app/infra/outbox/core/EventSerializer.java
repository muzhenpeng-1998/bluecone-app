// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/core/EventSerializer.java
package com.bluecone.app.infra.outbox.core;

import com.bluecone.app.core.event.DomainEvent;

import java.util.Map;

/**
 * 领域事件序列化/反序列化策略。
 *
 * <p>负责将 {@link DomainEvent} 转为 JSON 载荷与头部信息，并在出站时恢复对象。
 * 统一封装可替换 JSON/Binary/Avro 等实现。</p>
 */
public interface EventSerializer {

    /**
     * 序列化事件载荷。
     *
     * @param event 事件
     * @return JSON 字符串
     */
    String serializePayload(DomainEvent event);

    /**
     * 序列化事件头，通常包含类名、事件类型及元数据。
     *
     * @param event 事件
     * @return 头部键值对
     */
    Map<String, String> serializeHeaders(DomainEvent event);

    /**
     * 反序列化事件。
     *
     * @param payload   JSON 载荷
     * @param headers   头部信息（至少包含事件类名）
     * @return 事件实例
     */
    DomainEvent deserialize(String payload, Map<String, String> headers);
}
