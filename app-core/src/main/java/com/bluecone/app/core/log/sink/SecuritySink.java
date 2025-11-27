package com.bluecone.app.core.log.sink;

import com.bluecone.app.core.log.ApiEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 安全事件 Sink：针对登录/鉴权失败等敏感事件单独输出。
 */
@Component
public class SecuritySink implements EventSink {

    private static final Logger log = LoggerFactory.getLogger(SecuritySink.class);
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public boolean supports(ApiEvent event) {
        if (event == null) {
            return false;
        }
        boolean isError = event.getEventType() == ApiEvent.EventType.API_ERROR;
        boolean isAuthPath = StringUtils.hasText(event.getPath()) &&
                (event.getPath().contains("login") || event.getPath().contains("auth"));
        boolean hasAuthDigest = StringUtils.hasText(event.getExceptionDigest()) &&
                event.getExceptionDigest().toLowerCase().contains("auth");
        return isError && (isAuthPath || hasAuthDigest);
    }

    @Override
    public void publish(ApiEvent event) {
        try {
            log.warn("[SECURITY] {}", objectMapper.writeValueAsString(event));
        } catch (Exception ex) {
            log.error("Security sink serialization failed", ex);
        }
    }
}
