package com.bluecone.app.core.event.consume.api;

/**
 * 事件消费处理器，接受 {@link EventEnvelope} 并执行业务逻辑。
 */
@FunctionalInterface
public interface EventHandler {

    /**
     * 处理单条事件。
     *
     * @param event 事件 Envelope
     * @throws Exception 业务异常将由模板捕获并记录失败
     */
    void handle(EventEnvelope event) throws Exception;
}

