package com.bluecone.app.infra.notify.model;

import com.bluecone.app.core.notify.NotificationPriority;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 内部意图模型（Routing/Timeline 层入口）。
 *
 * <p>相比 API 的 {@code NotificationRequest}，携带 traceId / occurredAt 等上下文信息。</p>
 */
public class NotificationIntent {

    private final String scenarioCode;
    private final Long tenantId;
    private final Long triggeredByUserId;
    private final NotificationPriority priority;
    private final Map<String, Object> attributes;
    private final String idempotentKey;
    private final Instant occurredAt;
    private final String traceId;

    @JsonCreator
    public NotificationIntent(@JsonProperty("scenarioCode") String scenarioCode,
                              @JsonProperty("tenantId") Long tenantId,
                              @JsonProperty("triggeredByUserId") Long triggeredByUserId,
                              @JsonProperty("priority") NotificationPriority priority,
                              @JsonProperty("attributes") Map<String, Object> attributes,
                              @JsonProperty("idempotentKey") String idempotentKey,
                              @JsonProperty("occurredAt") Instant occurredAt,
                              @JsonProperty("traceId") String traceId) {
        this.scenarioCode = Objects.requireNonNull(scenarioCode, "scenarioCode must not be null");
        this.tenantId = tenantId;
        this.triggeredByUserId = triggeredByUserId;
        this.priority = priority == null ? NotificationPriority.NORMAL : priority;
        this.attributes = attributes == null ? Collections.emptyMap() : new HashMap<>(attributes);
        this.idempotentKey = idempotentKey;
        this.occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        this.traceId = traceId;
    }

    public String getScenarioCode() {
        return scenarioCode;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getTriggeredByUserId() {
        return triggeredByUserId;
    }

    public NotificationPriority getPriority() {
        return priority;
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public String getIdempotentKey() {
        return idempotentKey;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getTraceId() {
        return traceId;
    }
}
