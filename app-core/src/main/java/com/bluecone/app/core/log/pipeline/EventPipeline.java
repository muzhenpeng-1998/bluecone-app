package com.bluecone.app.core.log.pipeline;

import com.bluecone.app.core.log.ApiEvent;
import com.bluecone.app.core.log.sink.EventSink;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 事件处理流水线：补充 → 脱敏 → 限流 → 下沉。
 */
@Component
@RequiredArgsConstructor
public class EventPipeline {

    private static final Logger log = LoggerFactory.getLogger(EventPipeline.class);

    private final EventEnricher enricher;
    private final EventSanitizer sanitizer;
    private final EventLimiter limiter;
    private final List<EventSink> sinks;

    public void process(ApiEvent event) {
        if (event == null) {
            return;
        }

        ApiEvent enriched = enricher.enrich(event);
        ApiEvent sanitized = sanitizer.sanitize(enriched);

        if (!limiter.allow(sanitized)) {
            log.debug("ApiEvent dropped by limiter: {}", sanitized.getEventType());
            return;
        }

        for (EventSink sink : sinks) {
            try {
                if (sink.supports(sanitized)) {
                    sink.publish(sanitized);
                }
            } catch (Exception ex) {
                log.warn("Event sink failed: {}", sink.getClass().getSimpleName(), ex);
            }
        }
    }
}
