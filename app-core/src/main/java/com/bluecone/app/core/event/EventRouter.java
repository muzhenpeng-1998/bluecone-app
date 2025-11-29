// File: app-core/src/main/java/com/bluecone/app/core/event/EventRouter.java
package com.bluecone.app.core.event;

import java.util.List;

/**
 * 根据事件匹配需要处理的 handler。
 *
 * <p>可以基于事件类、事件名甚至模式匹配。当前实现采用基于 class 的简单路由，接口预留未来策略扩展。</p>
 */
public interface EventRouter {

    /**
     * 找到需要接收该事件的所有 handler。
     *
     * @param event 已发布的领域事件
     * @return 有序 handler 列表，若无返回空列表
     */
    List<EventHandler<?>> route(DomainEvent event);
}
