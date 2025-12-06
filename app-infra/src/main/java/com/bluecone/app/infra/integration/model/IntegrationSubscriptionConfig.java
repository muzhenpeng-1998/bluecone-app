package com.bluecone.app.infra.integration.model;

import com.bluecone.app.infra.integration.domain.IntegrationChannelType;

import java.util.Map;

/**
 * 供管理端/业务侧使用的订阅配置载体。
 */
public class IntegrationSubscriptionConfig {

    private Long id;
    private Long tenantId;
    private String eventType;
    private IntegrationChannelType channelType;
    private String targetUrl;
    private String secret;
    private Boolean enabled;
    private Integer maxRetry;
    private Integer timeoutMs;
    private Integer rateLimitQps;
    private Map<String, String> headers;
    private Map<String, Object> extraConfig;
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(final Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(final String eventType) {
        this.eventType = eventType;
    }

    public IntegrationChannelType getChannelType() {
        return channelType;
    }

    public void setChannelType(final IntegrationChannelType channelType) {
        this.channelType = channelType;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(final String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(final String secret) {
        this.secret = secret;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(final Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getMaxRetry() {
        return maxRetry;
    }

    public void setMaxRetry(final Integer maxRetry) {
        this.maxRetry = maxRetry;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(final Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getRateLimitQps() {
        return rateLimitQps;
    }

    public void setRateLimitQps(final Integer rateLimitQps) {
        this.rateLimitQps = rateLimitQps;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(final Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, Object> getExtraConfig() {
        return extraConfig;
    }

    public void setExtraConfig(final Map<String, Object> extraConfig) {
        this.extraConfig = extraConfig;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }
}
