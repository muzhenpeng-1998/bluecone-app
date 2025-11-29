// File: app-core/src/main/java/com/bluecone/app/core/event/EventSink.java
package com.bluecone.app.core.event;

/**
 * 处理完事件后的下游落地点。
 *
 * <p>当前仅提供日志 sink；未来可并行接入 Kafka、Redis Stream、审计表或 Outbox 确保可靠投递。</p>
 */
public interface EventSink {

    /**
     * 将事件投递到外部系统或观测通道。
     *
     * @param event 已处理的领域事件
     */
    void deliver(DomainEvent event);
}
