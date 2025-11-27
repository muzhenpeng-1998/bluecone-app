package com.bluecone.app.core.log.error.sink;

import com.bluecone.app.core.log.error.ExceptionEvent;

/**
 * 异常事件下沉接口。
 */
public interface ExceptionSink {

    void publish(ExceptionEvent event);
}
