package com.bluecone.app.infra.integration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bluecone.app.infra.integration.domain.IntegrationChannelType;
import com.bluecone.app.infra.integration.domain.IntegrationDeliveryStatus;

import java.time.LocalDateTime;

/**
 * 投递任务实体，对应表 bc_integration_delivery。
 */
@TableName("bc_integration_delivery")
public class IntegrationDeliveryEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long subscriptionId;

    private Long tenantId;

    private String eventId;

    private String eventType;

    private String payload;

    private String headers;

    private IntegrationChannelType channelType;

    private IntegrationDeliveryStatus status;

    private Integer retryCount;

    private LocalDateTime nextRetryAt;

    private String lastError;

    private Integer lastHttpStatus;

    private Integer lastDurationMs;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(final Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(final Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(final String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(final String payload) {
        this.payload = payload;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(final String headers) {
        this.headers = headers;
    }

    public IntegrationChannelType getChannelType() {
        return channelType;
    }

    public void setChannelType(final IntegrationChannelType channelType) {
        this.channelType = channelType;
    }

    public IntegrationDeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(final IntegrationDeliveryStatus status) {
        this.status = status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(final Integer retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(final LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(final String lastError) {
        this.lastError = lastError;
    }

    public Integer getLastHttpStatus() {
        return lastHttpStatus;
    }

    public void setLastHttpStatus(final Integer lastHttpStatus) {
        this.lastHttpStatus = lastHttpStatus;
    }

    public Integer getLastDurationMs() {
        return lastDurationMs;
    }

    public void setLastDurationMs(final Integer lastDurationMs) {
        this.lastDurationMs = lastDurationMs;
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
