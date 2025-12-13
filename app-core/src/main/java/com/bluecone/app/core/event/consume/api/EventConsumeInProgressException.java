package com.bluecone.app.core.event.consume.api;

/**
 * 事件消费仍在进行中的异常。
 *
 * <p>可选使用，模板也可以通过 {@link ConsumeResult#inProgress()} 返回状态。</p>
 */
public class EventConsumeInProgressException extends RuntimeException {

    public EventConsumeInProgressException(String message) {
        super(message);
    }
}

