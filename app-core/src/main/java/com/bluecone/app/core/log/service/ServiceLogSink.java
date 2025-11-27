package com.bluecone.app.core.log.service;

import com.bluecone.app.core.log.ApiEvent;
import com.bluecone.app.core.log.sink.EventSink;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Service 事件日志 Sink：输出结构化 JSON，复用统一事件管道。
 */
@Component
public class ServiceLogSink implements EventSink {

    private static final Logger log = LoggerFactory.getLogger(ServiceLogSink.class);
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public boolean supports(ApiEvent event) {
        return event instanceof ServiceEvent;
    }

    @Override
    public void publish(ApiEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            if (event instanceof ServiceExceptionEvent) {
                log.error(json);
            } else {
                log.info(json);
            }
        } catch (Exception ex) {
            log.warn("Failed to serialize ServiceEvent", ex);
        }
    }
}
