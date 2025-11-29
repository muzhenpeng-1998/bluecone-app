// File: app-core/src/main/java/com/bluecone/app/core/event/EventHandler.java
package com.bluecone.app.core.event;

/**
 * 针对某个领域事件的业务处理器。
 *
 * @param <E> 具体事件类型
 *
 * <p>建议保持单一职责，不直接控制事务边界，编排与基础设施交给上层。</p>
 */
public interface EventHandler<E extends DomainEvent> {

    /**
     * 处理事件。
     *
     * @param event 不可变领域事件
     */
    void handle(E event);
}
