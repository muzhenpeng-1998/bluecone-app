package com.bluecone.app.core.log.service;

import com.bluecone.app.core.log.ApiEvent;
import com.bluecone.app.core.log.sink.EventSink;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Service 事件指标 Sink：对服务方法调用与耗时打点。
 */
@Component
public class ServiceMetricsSink implements EventSink {

    private final MeterRegistry meterRegistry;

    public ServiceMetricsSink(@Autowired(required = false) MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean supports(ApiEvent event) {
        return meterRegistry != null && event instanceof ServiceEvent;
    }

    @Override
    public void publish(ApiEvent event) {
        if (!(event instanceof ServiceEvent serviceEvent) || meterRegistry == null) {
            return;
        }
        Tags tags = Tags.of(
                "service", safe(serviceEvent.getServiceClass()),
                "method", safe(serviceEvent.getServiceMethod()),
                "outcome", safe(serviceEvent.getOutcome())
        );
        meterRegistry.counter("service.call.total", tags).increment();
        if (serviceEvent.getElapsedMs() != null) {
            meterRegistry.timer("service.call.latency", tags)
                    .record(serviceEvent.getElapsedMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    private String safe(String value) {
        return value == null ? "unknown" : value;
    }
}
