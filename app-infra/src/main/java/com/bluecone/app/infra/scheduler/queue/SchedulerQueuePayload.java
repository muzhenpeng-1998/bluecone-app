package com.bluecone.app.infra.scheduler.queue;

import java.time.LocalDateTime;

/**
 * 队列消息载荷，驱动执行 Worker。
 */
public class SchedulerQueuePayload {

    private String code;
    private String traceId;
    private String tenantId;
    private LocalDateTime scheduledAt;

    public SchedulerQueuePayload() {
    }

    public SchedulerQueuePayload(String code, String traceId, String tenantId, LocalDateTime scheduledAt) {
        this.code = code;
        this.traceId = traceId;
        this.tenantId = tenantId;
        this.scheduledAt = scheduledAt;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }
}
