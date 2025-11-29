// File: app-core/src/main/java/com/bluecone/app/core/event/EventPipeline.java
package com.bluecone.app.core.event;

/**
 * 路由前的管道扩展，用于观测、校验等中间处理。
 *
 * <p>当前建议不修改事件本身，专注日志、监控、安全。若未来需要修改，可采用写时复制保持不可变性。</p>
 */
public interface EventPipeline {

    /**
     * 处理进入管道的事件。
     *
     * @param event 不可变领域事件
     * @return 建议返回原事件（未来如需转换，可返回安全副本）
     */
    DomainEvent process(DomainEvent event);
}
