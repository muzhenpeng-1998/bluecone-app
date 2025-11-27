package com.bluecone.app.core.log.sink;

import com.bluecone.app.core.log.ApiEvent;

/**
 * 事件下沉接口，支持可插拔 Sink。
 */
public interface EventSink {

    boolean supports(ApiEvent event);

    void publish(ApiEvent event);
}
