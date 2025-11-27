package com.bluecone.app.core.log.error;

import com.bluecone.app.core.log.error.sink.ExceptionSink;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 异常事件流水线：富化 → 脱敏 → 限流 → 下沉。
 */
@Component
@RequiredArgsConstructor
public class ExceptionEventPipeline {

    private static final Logger log = LoggerFactory.getLogger(ExceptionEventPipeline.class);

    private final ExceptionEnricher enricher;
    private final ExceptionSanitizer sanitizer;
    private final ExceptionLimiter limiter;
    private final List<ExceptionSink> sinks;

    public void process(ExceptionEvent event) {
        if (event == null) {
            return;
        }
        ExceptionEvent enriched = enricher.enrich(event);
        ExceptionEvent sanitized = sanitizer.sanitize(enriched);
        if (!limiter.allow(sanitized)) {
            log.debug("ExceptionEvent dropped by limiter: traceId={} path={} code={}",
                    sanitized.getTraceId(), sanitized.getPath(), sanitized.getErrorCode());
            return;
        }
        for (ExceptionSink sink : sinks) {
            try {
                sink.publish(sanitized);
            } catch (Exception ex) {
                log.warn("Exception sink failed: {}", sink.getClass().getSimpleName(), ex);
            }
        }
    }
}
