package com.bluecone.app.core.notify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 通知统一入参（API 层）。
 *
 * <p>业务调用者只需要关注场景、租户、触发人和业务属性，内部会自动完成路由、限流、Outbox 等。</p>
 */
public class NotificationRequest {

    private String scenarioCode;
    private Long tenantId;
    private Long triggeredByUserId;
    private NotificationPriority priority;
    private Map<String, Object> attributes;
    private String idempotentKey;

    public NotificationRequest() {
    }

    public NotificationRequest(String scenarioCode,
                               Long tenantId,
                               Long triggeredByUserId,
                               NotificationPriority priority,
                               Map<String, Object> attributes,
                               String idempotentKey) {
        this.scenarioCode = Objects.requireNonNull(scenarioCode, "scenarioCode must not be null");
        this.tenantId = tenantId;
        this.triggeredByUserId = triggeredByUserId;
        this.priority = priority;
        this.attributes = attributes == null ? Collections.emptyMap() : new HashMap<>(attributes);
        this.idempotentKey = idempotentKey;
    }

    public String getScenarioCode() {
        return scenarioCode;
    }

    public void setScenarioCode(String scenarioCode) {
        this.scenarioCode = scenarioCode;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getTriggeredByUserId() {
        return triggeredByUserId;
    }

    public void setTriggeredByUserId(Long triggeredByUserId) {
        this.triggeredByUserId = triggeredByUserId;
    }

    public NotificationPriority getPriority() {
        return priority;
    }

    public void setPriority(NotificationPriority priority) {
        this.priority = priority;
    }

    public Map<String, Object> getAttributes() {
        return attributes == null ? Collections.emptyMap() : Collections.unmodifiableMap(attributes);
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes == null ? Collections.emptyMap() : new HashMap<>(attributes);
    }

    public String getIdempotentKey() {
        return idempotentKey;
    }

    public void setIdempotentKey(String idempotentKey) {
        this.idempotentKey = idempotentKey;
    }
}
