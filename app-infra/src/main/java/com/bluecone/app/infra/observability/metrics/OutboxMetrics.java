package com.bluecone.app.infra.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Outbox Metrics
 * 
 * Provides custom metrics for Outbox event publishing:
 * - Counter: outbox_events_published_total (by status: success/failure)
 * - Counter: outbox_events_dispatched_total (by event type)
 * - Counter: outbox_events_retry_total (by retry count)
 * - Gauge: outbox_events_pending_count (pending events in queue)
 * - Timer: outbox_dispatch_duration_seconds (dispatch latency)
 */
@Slf4j
@Component
public class OutboxMetrics {

    private final MeterRegistry registry;
    
    // Counters
    private final Counter publishedSuccessCounter;
    private final Counter publishedFailureCounter;
    private final Counter retryCounter;
    
    // Gauge
    private final AtomicLong pendingEventsCount;
    
    // Timer
    private final Timer dispatchTimer;

    public OutboxMetrics(MeterRegistry registry) {
        this.registry = registry;
        
        // Initialize counters
        this.publishedSuccessCounter = Counter.builder("outbox_events_published_total")
                .description("Total number of outbox events published successfully")
                .tag("status", "success")
                .register(registry);
        
        this.publishedFailureCounter = Counter.builder("outbox_events_published_total")
                .description("Total number of outbox events that failed to publish")
                .tag("status", "failure")
                .register(registry);
        
        this.retryCounter = Counter.builder("outbox_events_retry_total")
                .description("Total number of outbox event retries")
                .register(registry);
        
        // Initialize gauge
        this.pendingEventsCount = new AtomicLong(0);
        Gauge.builder("outbox_events_pending_count", pendingEventsCount, AtomicLong::get)
                .description("Current number of pending outbox events")
                .register(registry);
        
        // Initialize timer
        this.dispatchTimer = Timer.builder("outbox_dispatch_duration_seconds")
                .description("Time taken to dispatch outbox events")
                .register(registry);
    }

    /**
     * Record a successful event publication
     */
    public void recordPublishSuccess() {
        publishedSuccessCounter.increment();
    }

    /**
     * Record a successful event publication with event type
     */
    public void recordPublishSuccess(String eventType) {
        Counter.builder("outbox_events_dispatched_total")
                .description("Total number of outbox events dispatched by type")
                .tag("event_type", eventType)
                .register(registry)
                .increment();
        publishedSuccessCounter.increment();
    }

    /**
     * Record a failed event publication
     */
    public void recordPublishFailure() {
        publishedFailureCounter.increment();
    }

    /**
     * Record a failed event publication with event type
     */
    public void recordPublishFailure(String eventType) {
        Counter.builder("outbox_events_dispatched_total")
                .description("Total number of outbox events dispatched by type")
                .tag("event_type", eventType)
                .tag("status", "failure")
                .register(registry)
                .increment();
        publishedFailureCounter.increment();
    }

    /**
     * Record an event retry
     */
    public void recordRetry() {
        retryCounter.increment();
    }

    /**
     * Record an event retry with retry count
     */
    public void recordRetry(int retryCount) {
        Counter.builder("outbox_events_retry_total")
                .description("Total number of outbox event retries")
                .tag("retry_count", String.valueOf(retryCount))
                .register(registry)
                .increment();
    }

    /**
     * Update pending events count
     */
    public void setPendingEventsCount(long count) {
        pendingEventsCount.set(count);
    }

    /**
     * Record dispatch duration
     */
    public void recordDispatchDuration(Runnable task) {
        dispatchTimer.record(task);
    }

    /**
     * Get timer for manual recording
     */
    public Timer.Sample startDispatchTimer() {
        return Timer.start(registry);
    }

    /**
     * Stop timer and record duration
     */
    public void stopDispatchTimer(Timer.Sample sample) {
        sample.stop(dispatchTimer);
    }
}
