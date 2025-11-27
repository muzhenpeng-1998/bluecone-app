package com.bluecone.app.core.log.sink;

import com.bluecone.app.core.log.ApiEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 默认日志 Sink：输出结构化 JSON。
 */
@Component
public class LogSink implements EventSink {

    private static final Logger log = LoggerFactory.getLogger(LogSink.class);
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public boolean supports(ApiEvent event) {
        return true;
    }

    @Override
    public void publish(ApiEvent event) {
        try {
            log.info(objectMapper.writeValueAsString(event));
        } catch (Exception ex) {
            log.warn("Failed to serialize ApiEvent", ex);
        }
    }
}
