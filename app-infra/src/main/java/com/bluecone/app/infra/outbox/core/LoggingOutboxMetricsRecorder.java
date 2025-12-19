// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/core/LoggingOutboxMetricsRecorder.java
package com.bluecone.app.infra.outbox.core;

import com.bluecone.app.infra.observability.metrics.OutboxMetrics;
import com.bluecone.app.infra.outbox.entity.OutboxMessageEntity;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * 默认的日志型指标记录器，结构化输出关键字段。
 */
@Component
public class LoggingOutboxMetricsRecorder implements OutboxMetricsRecorder {

    private static final Logger log = LoggerFactory.getLogger(LoggingOutboxMetricsRecorder.class);
    private final MeterRegistry meterRegistry;
    private final OutboxMetrics outboxMetrics;

    public LoggingOutboxMetricsRecorder(final MeterRegistry meterRegistry,
                                        final OutboxMetrics outboxMetrics) {
        this.meterRegistry = meterRegistry;
        this.outboxMetrics = outboxMetrics;
    }

    @Override
    public void onCreated(final OutboxMessageEntity message) {
        // Populate MDC with outbox event ID for structured logging
        MDC.put("outboxEventId", String.valueOf(message.getId()));
        
        log.info("[OutboxMetric] created id={} eventType={} tenantId={} status={} retry={}",
                message.getId(), message.getEventType(), message.getTenantId(), message.getStatus(), message.getRetryCount());
        count("CREATED", message);
    }

    @Override
    public void onPublishedSuccess(final OutboxMessageEntity message) {
        // Populate MDC with outbox event ID for structured logging
        MDC.put("outboxEventId", String.valueOf(message.getId()));
        
        log.info("[OutboxMetric] success id={} eventType={} tenantId={} status={} retry={}",
                message.getId(), message.getEventType(), message.getTenantId(), message.getStatus(), message.getRetryCount());
        count("SUCCESS", message);
        
        // Record metrics using OutboxMetrics
        outboxMetrics.recordPublishSuccess(message.getEventType());
    }

    @Override
    public void onPublishedFailure(final OutboxMessageEntity message, final Throwable error) {
        // Populate MDC with outbox event ID for structured logging
        MDC.put("outboxEventId", String.valueOf(message.getId()));
        
        log.warn("[OutboxMetric] failure id={} eventType={} tenantId={} status={} retry={} error={}",
                message.getId(), message.getEventType(), message.getTenantId(), message.getStatus(), message.getRetryCount(),
                error != null ? error.getMessage() : null);
        count("FAILURE", message);
        
        // Record metrics using OutboxMetrics
        outboxMetrics.recordPublishFailure(message.getEventType());
        if (message.getRetryCount() != null && message.getRetryCount() > 0) {
            outboxMetrics.recordRetry(message.getRetryCount());
        }
    }

    @Override
    public void onDeadLetter(final OutboxMessageEntity message, final Throwable error) {
        // Populate MDC with outbox event ID for structured logging
        MDC.put("outboxEventId", String.valueOf(message.getId()));
        
        log.error("[OutboxMetric] dead-letter id={} eventType={} tenantId={} retry={} error={}",
                message.getId(), message.getEventType(), message.getTenantId(), message.getRetryCount(),
                error != null ? error.getMessage() : null);
        count("DEAD", message);
        
        // Record metrics using OutboxMetrics
        outboxMetrics.recordPublishFailure(message.getEventType());
    }

    private void count(final String result, final OutboxMessageEntity message) {
        String topic = message.getEventType() == null ? "UNKNOWN" : message.getEventType();
        meterRegistry.counter("bluecone_outbox_publish_total",
                "topic", topic,
                "result", result).increment();
    }
}
