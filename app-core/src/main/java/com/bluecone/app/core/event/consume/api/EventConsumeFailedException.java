package com.bluecone.app.core.event.consume.api;

/**
 * 事件消费失败异常，当等待模式下观察到最终状态为 FAILED 时抛出。
 */
public class EventConsumeFailedException extends RuntimeException {

    public EventConsumeFailedException(String message) {
        super(message);
    }

    public EventConsumeFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}

