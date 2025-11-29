// File: app-core/src/main/java/com/bluecone/app/core/event/DomainEventPublisher.java
package com.bluecone.app.core.event;

/**
 * 领域层唯一依赖的事件发布入口。
 *
 * <p>调用方只关心发布语义，不关心底层实现是事务内 Outbox、MQ 还是内存同步。
 * 不同环境可以切换不同实现，业务代码保持一致。</p>
 */
public interface DomainEventPublisher {

    /**
     * 发布单个领域事件。
     *
     * @param event 领域事件
     */
    void publish(DomainEvent event);
}
