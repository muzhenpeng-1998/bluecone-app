package com.bluecone.app.core.log.error.sink;

import com.bluecone.app.core.log.error.ExceptionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 默认异常日志 Sink：输出结构化 JSON 到应用日志。
 */
@Component
public class LogExceptionSink implements ExceptionSink {

    private static final Logger log = LoggerFactory.getLogger(LogExceptionSink.class);
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public void publish(ExceptionEvent event) {
        if (event == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(event);
            log.error("[EXCEPTION] type={} traceId={} path={} code={} message={}; payload={}",
                    event.getEventType(),
                    event.getTraceId(),
                    event.getPath(),
                    event.getErrorCode(),
                    event.getMessage(),
                    json);
        } catch (Exception ex) {
            log.error("Failed to serialize ExceptionEvent", ex);
        }
    }
}
