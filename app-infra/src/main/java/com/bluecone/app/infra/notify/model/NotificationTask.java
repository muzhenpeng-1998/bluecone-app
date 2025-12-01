package com.bluecone.app.infra.notify.model;

import com.bluecone.app.core.notify.NotificationPriority;
import com.bluecone.app.infra.notify.policy.NotifyChannel;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 单通道任务（Routing 层产出，Timeline/Delivery 消费）。
 */
public class NotificationTask {

    private final NotifyChannel channel;
    private final String templateCode;
    private final Map<String, Object> variables;
    private final NotificationPriority priority;
    private final String idempotentKey;
    private final Long tenantId;
    private final String scenarioCode;
    private final Integer maxPerMinute;
    private final String channelConfigId;

    @JsonCreator
    public NotificationTask(@JsonProperty("channel") NotifyChannel channel,
                            @JsonProperty("templateCode") String templateCode,
                            @JsonProperty("variables") Map<String, Object> variables,
                            @JsonProperty("priority") NotificationPriority priority,
                            @JsonProperty("idempotentKey") String idempotentKey,
                            @JsonProperty("tenantId") Long tenantId,
                            @JsonProperty("scenarioCode") String scenarioCode,
                            @JsonProperty("maxPerMinute") Integer maxPerMinute,
                            @JsonProperty("channelConfigId") String channelConfigId) {
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.templateCode = templateCode;
        this.variables = variables == null ? Collections.emptyMap() : new HashMap<>(variables);
        this.priority = priority == null ? NotificationPriority.NORMAL : priority;
        this.idempotentKey = idempotentKey;
        this.tenantId = tenantId;
        this.scenarioCode = scenarioCode;
        this.maxPerMinute = maxPerMinute;
        this.channelConfigId = channelConfigId;
    }

    public NotifyChannel getChannel() {
        return channel;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public Map<String, Object> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    public NotificationPriority getPriority() {
        return priority;
    }

    public String getIdempotentKey() {
        return idempotentKey;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getScenarioCode() {
        return scenarioCode;
    }

    public Integer getMaxPerMinute() {
        return maxPerMinute;
    }

    public String getChannelConfigId() {
        return channelConfigId;
    }
}
