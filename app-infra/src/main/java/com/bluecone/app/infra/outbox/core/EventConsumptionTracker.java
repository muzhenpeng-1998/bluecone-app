// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/core/EventConsumptionTracker.java
package com.bluecone.app.infra.outbox.core;

/**
 * 事件消费幂等跟踪器，防止重复处理。
 */
public interface EventConsumptionTracker {

    /**
     * 尝试标记某个消费者正在处理该事件。
     *
     * @param consumerName 消费者标识（如 handler 名称）
     * @param eventId      事件 ID
     * @return 首次处理返回 true；已处理过返回 false
     */
    boolean tryMarkProcessing(String consumerName, String eventId);
}
