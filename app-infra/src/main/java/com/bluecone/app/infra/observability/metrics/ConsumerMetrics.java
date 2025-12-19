package com.bluecone.app.infra.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Consumer Metrics
 * 
 * Provides custom metrics for event consumers:
 * - Counter: consumer_events_processed_total (by event type and status)
 * - Counter: consumer_events_deduped_total (deduplicated events)
 * - Timer: consumer_processing_duration_seconds (processing latency by event type)
 */
@Slf4j
@Component
public class ConsumerMetrics {

    private final MeterRegistry registry;

    public ConsumerMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Record a successful event consumption
     */
    public void recordProcessSuccess(String eventType) {
        Counter.builder("consumer_events_processed_total")
                .description("Total number of events processed by consumers")
                .tag("event_type", eventType)
                .tag("status", "success")
                .register(registry)
                .increment();
    }

    /**
     * Record a failed event consumption
     */
    public void recordProcessFailure(String eventType) {
        Counter.builder("consumer_events_processed_total")
                .description("Total number of events processed by consumers")
                .tag("event_type", eventType)
                .tag("status", "failure")
                .register(registry)
                .increment();
    }

    /**
     * Record a deduplicated event (already processed)
     */
    public void recordDeduped(String eventType) {
        Counter.builder("consumer_events_deduped_total")
                .description("Total number of deduplicated events")
                .tag("event_type", eventType)
                .register(registry)
                .increment();
    }

    /**
     * Record processing duration
     */
    public void recordProcessingDuration(String eventType, Runnable task) {
        Timer.builder("consumer_processing_duration_seconds")
                .description("Time taken to process events by type")
                .tag("event_type", eventType)
                .register(registry)
                .record(task);
    }

    /**
     * Get timer for manual recording
     */
    public Timer.Sample startProcessingTimer() {
        return Timer.start(registry);
    }

    /**
     * Stop timer and record duration
     */
    public void stopProcessingTimer(Timer.Sample sample, String eventType) {
        sample.stop(Timer.builder("consumer_processing_duration_seconds")
                .description("Time taken to process events by type")
                .tag("event_type", eventType)
                .register(registry));
    }
}
