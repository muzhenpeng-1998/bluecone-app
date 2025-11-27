package com.bluecone.app.core.log.sink;

import com.bluecone.app.core.log.ApiEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Micrometer 指标 Sink：统计调用与错误次数。
 */
@Component
public class MetricsSink implements EventSink {

    private final MeterRegistry meterRegistry;

    public MetricsSink(@Autowired(required = false) MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean supports(ApiEvent event) {
        return meterRegistry != null;
    }

    @Override
    public void publish(ApiEvent event) {
        if (meterRegistry == null || event == null) {
            return;
        }
        Tags tags = Tags.of(
                "path", safe(event.getPath()),
                "method", safe(event.getMethod()),
                "version", safe(event.getVersion()),
                "status", event.getStatus() != null ? event.getStatus().name() : "UNKNOWN"
        );

        meterRegistry.counter("api.events.total", tags).increment();
        if (event.getEventType() == ApiEvent.EventType.API_ERROR) {
            meterRegistry.counter("api.events.error", tags).increment();
        }
    }

    private String safe(String value) {
        return value == null ? "unknown" : value;
    }
}
