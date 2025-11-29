// File: app-core/src/main/java/com/bluecone/app/core/event/EventOrchestrator.java
package com.bluecone.app.core.event;

/**
 * 对业务开放的事件发布入口。
 *
 * <p>业务侧只依赖此接口；无论底层是内存同步、异步线程池、MQ 还是 Outbox，都保持调用方式不变。
 * 目标是一行代码：{@code eventOrchestrator.fire(new OrderPaidEvent(...));}</p>
 */
public interface EventOrchestrator {

    /**
     * 将单个领域事件送入 pipeline、handler、sink 全链路。
     *
     * @param event 不可变领域事件
     */
    void fire(DomainEvent event);
}
