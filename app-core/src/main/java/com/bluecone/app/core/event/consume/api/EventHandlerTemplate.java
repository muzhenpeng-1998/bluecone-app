package com.bluecone.app.core.event.consume.api;

/**
 * 事件消费处理模板，封装幂等、并发控制与失败重试逻辑。
 */
public interface EventHandlerTemplate {

    /**
     * 在消费去重模板下处理事件。
     *
     * @param consumerGroup 消费者组标识
     * @param event         事件 Envelope
     * @param handler       业务处理回调
     * @param options       消费选项
     * @return 消费结果
     */
    ConsumeResult consume(String consumerGroup, EventEnvelope event, EventHandler handler, ConsumeOptions options);
}

