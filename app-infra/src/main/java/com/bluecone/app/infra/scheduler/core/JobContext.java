package com.bluecone.app.infra.scheduler.core;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

/**
 * 任务执行上下文，贯穿整个执行链路。
 */
public class JobContext {

    private final String code;
    private final String name;
    private final String cronExpr;
    private final int timeoutSeconds;
    private final String tenantId;
    private final String traceId;
    private final LocalDateTime scheduledTime;
    private final Map<String, Object> attributes;

    private LocalDateTime startedAt;

    public JobContext(String code,
                      String name,
                      String cronExpr,
                      int timeoutSeconds,
                      String tenantId,
                      String traceId,
                      LocalDateTime scheduledTime,
                      Map<String, Object> attributes) {
        this.code = code;
        this.name = name;
        this.cronExpr = cronExpr;
        this.timeoutSeconds = timeoutSeconds;
        this.tenantId = tenantId;
        this.traceId = traceId;
        this.scheduledTime = scheduledTime;
        this.attributes = attributes == null ? Collections.emptyMap() : Collections.unmodifiableMap(attributes);
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCronExpr() {
        return cronExpr;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTraceId() {
        return traceId;
    }

    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void markStarted(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
}
