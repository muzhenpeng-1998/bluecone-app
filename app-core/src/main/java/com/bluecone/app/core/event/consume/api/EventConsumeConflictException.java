package com.bluecone.app.core.event.consume.api;

/**
 * 事件消费冲突异常：同一 consumer_group + event_id 但 event_type 不一致等数据污染场景。
 */
public class EventConsumeConflictException extends RuntimeException {

    public EventConsumeConflictException(String message) {
        super(message);
    }

    public EventConsumeConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

