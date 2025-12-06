package com.bluecone.app.infra.integration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bluecone.app.infra.integration.domain.IntegrationChannelType;

import java.time.LocalDateTime;

/**
 * 集成订阅配置实体，对应表 bc_integration_subscription。
 */
@TableName("bc_integration_subscription")
public class IntegrationSubscriptionEntity {

    @TableId(type = IdType.AUTO)
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

    private String headers;

    private String extraConfig;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

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

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(final String headers) {
        this.headers = headers;
    }

    public String getExtraConfig() {
        return extraConfig;
    }

    public void setExtraConfig(final String extraConfig) {
        this.extraConfig = extraConfig;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
