package com.bluecone.app.notify.infrastructure.converter;

import com.bluecone.app.notify.api.enums.NotificationChannel;
import com.bluecone.app.notify.api.enums.NotificationTaskStatus;
import com.bluecone.app.notify.api.enums.TemplateStatus;
import com.bluecone.app.notify.domain.model.*;
import com.bluecone.app.notify.infrastructure.dao.*;

/**
 * 通知模块转换器
 */
public class NotifyConverter {
    
    // ===== NotifyTemplate 转换 =====
    
    public static NotifyTemplate toDomain(NotifyTemplateDO dataObject) {
        if (dataObject == null) {
            return null;
        }
        return NotifyTemplate.builder()
                .id(dataObject.getId())
                .tenantId(dataObject.getTenantId())
                .templateCode(dataObject.getTemplateCode())
                .templateName(dataObject.getTemplateName())
                .bizType(dataObject.getBizType())
                .channel(NotificationChannel.valueOf(dataObject.getChannel()))
                .titleTemplate(dataObject.getTitleTemplate())
                .contentTemplate(dataObject.getContentTemplate())
                .templateVariablesJson(dataObject.getTemplateVariables())
                .status(TemplateStatus.valueOf(dataObject.getStatus()))
                .priority(dataObject.getPriority())
                .rateLimitConfigJson(dataObject.getRateLimitConfig())
                .createdBy(dataObject.getCreatedBy())
                .updatedBy(dataObject.getUpdatedBy())
                .createdAt(dataObject.getCreatedAt())
                .updatedAt(dataObject.getUpdatedAt())
                .build();
    }
    
    public static NotifyTemplateDO toDO(NotifyTemplate domain) {
        if (domain == null) {
            return null;
        }
        NotifyTemplateDO dataObject = new NotifyTemplateDO();
        dataObject.setId(domain.getId());
        dataObject.setTenantId(domain.getTenantId());
        dataObject.setTemplateCode(domain.getTemplateCode());
        dataObject.setTemplateName(domain.getTemplateName());
        dataObject.setBizType(domain.getBizType());
        dataObject.setChannel(domain.getChannel().name());
        dataObject.setTitleTemplate(domain.getTitleTemplate());
        dataObject.setContentTemplate(domain.getContentTemplate());
        dataObject.setTemplateVariables(domain.getTemplateVariablesJson());
        dataObject.setStatus(domain.getStatus().name());
        dataObject.setPriority(domain.getPriority());
        dataObject.setRateLimitConfig(domain.getRateLimitConfigJson());
        dataObject.setCreatedBy(domain.getCreatedBy());
        dataObject.setUpdatedBy(domain.getUpdatedBy());
        dataObject.setCreatedAt(domain.getCreatedAt());
        dataObject.setUpdatedAt(domain.getUpdatedAt());
        return dataObject;
    }
    
    // ===== NotifyTask 转换 =====
    
    public static NotifyTask toDomain(NotifyTaskDO dataObject) {
        if (dataObject == null) {
            return null;
        }
        return NotifyTask.builder()
                .id(dataObject.getId())
                .tenantId(dataObject.getTenantId())
                .userId(dataObject.getUserId())
                .bizType(dataObject.getBizType())
                .bizId(dataObject.getBizId())
                .templateCode(dataObject.getTemplateCode())
                .channel(NotificationChannel.valueOf(dataObject.getChannel()))
                .priority(dataObject.getPriority())
                .variablesJson(dataObject.getVariables())
                .title(dataObject.getTitle())
                .content(dataObject.getContent())
                .idempotencyKey(dataObject.getIdempotencyKey())
                .status(NotificationTaskStatus.valueOf(dataObject.getStatus()))
                .retryCount(dataObject.getRetryCount())
                .maxRetryCount(dataObject.getMaxRetryCount())
                .nextRetryAt(dataObject.getNextRetryAt())
                .lastError(dataObject.getLastError())
                .rateLimitCheckedAt(dataObject.getRateLimitCheckedAt())
                .rateLimitPassed(dataObject.getRateLimitPassed())
                .createdAt(dataObject.getCreatedAt())
                .updatedAt(dataObject.getUpdatedAt())
                .sentAt(dataObject.getSentAt())
                .build();
    }
    
    public static NotifyTaskDO toDO(NotifyTask domain) {
        if (domain == null) {
            return null;
        }
        NotifyTaskDO dataObject = new NotifyTaskDO();
        dataObject.setId(domain.getId());
        dataObject.setTenantId(domain.getTenantId());
        dataObject.setUserId(domain.getUserId());
        dataObject.setBizType(domain.getBizType());
        dataObject.setBizId(domain.getBizId());
        dataObject.setTemplateCode(domain.getTemplateCode());
        dataObject.setChannel(domain.getChannel().name());
        dataObject.setPriority(domain.getPriority());
        dataObject.setVariables(domain.getVariablesJson());
        dataObject.setTitle(domain.getTitle());
        dataObject.setContent(domain.getContent());
        dataObject.setIdempotencyKey(domain.getIdempotencyKey());
        dataObject.setStatus(domain.getStatus().name());
        dataObject.setRetryCount(domain.getRetryCount());
        dataObject.setMaxRetryCount(domain.getMaxRetryCount());
        dataObject.setNextRetryAt(domain.getNextRetryAt());
        dataObject.setLastError(domain.getLastError());
        dataObject.setRateLimitCheckedAt(domain.getRateLimitCheckedAt());
        dataObject.setRateLimitPassed(domain.getRateLimitPassed());
        dataObject.setCreatedAt(domain.getCreatedAt());
        dataObject.setUpdatedAt(domain.getUpdatedAt());
        dataObject.setSentAt(domain.getSentAt());
        return dataObject;
    }
    
    // ===== NotifySendLog 转换 =====
    
    public static NotifySendLog toDomain(NotifySendLogDO dataObject) {
        if (dataObject == null) {
            return null;
        }
        return NotifySendLog.builder()
                .id(dataObject.getId())
                .taskId(dataObject.getTaskId())
                .tenantId(dataObject.getTenantId())
                .userId(dataObject.getUserId())
                .channel(NotificationChannel.valueOf(dataObject.getChannel()))
                .bizType(dataObject.getBizType())
                .bizId(dataObject.getBizId())
                .title(dataObject.getTitle())
                .content(dataObject.getContent())
                .recipient(dataObject.getRecipient())
                .sendStatus(dataObject.getSendStatus())
                .errorCode(dataObject.getErrorCode())
                .errorMessage(dataObject.getErrorMessage())
                .providerResponseJson(dataObject.getProviderResponse())
                .sendDurationMs(dataObject.getSendDurationMs())
                .sentAt(dataObject.getSentAt())
                .createdAt(dataObject.getCreatedAt())
                .build();
    }
    
    public static NotifySendLogDO toDO(NotifySendLog domain) {
        if (domain == null) {
            return null;
        }
        NotifySendLogDO dataObject = new NotifySendLogDO();
        dataObject.setId(domain.getId());
        dataObject.setTaskId(domain.getTaskId());
        dataObject.setTenantId(domain.getTenantId());
        dataObject.setUserId(domain.getUserId());
        dataObject.setChannel(domain.getChannel().name());
        dataObject.setBizType(domain.getBizType());
        dataObject.setBizId(domain.getBizId());
        dataObject.setTitle(domain.getTitle());
        dataObject.setContent(domain.getContent());
        dataObject.setRecipient(domain.getRecipient());
        dataObject.setSendStatus(domain.getSendStatus());
        dataObject.setErrorCode(domain.getErrorCode());
        dataObject.setErrorMessage(domain.getErrorMessage());
        dataObject.setProviderResponse(domain.getProviderResponseJson());
        dataObject.setSendDurationMs(domain.getSendDurationMs());
        dataObject.setSentAt(domain.getSentAt());
        dataObject.setCreatedAt(domain.getCreatedAt());
        return dataObject;
    }
    
    // ===== UserPreference 转换 =====
    
    public static UserPreference toDomain(UserPreferenceDO dataObject) {
        if (dataObject == null) {
            return null;
        }
        return UserPreference.builder()
                .id(dataObject.getId())
                .tenantId(dataObject.getTenantId())
                .userId(dataObject.getUserId())
                .channelPreferencesJson(dataObject.getChannelPreferences())
                .quietHoursEnabled(dataObject.getQuietHoursEnabled())
                .quietHoursStart(dataObject.getQuietHoursStart())
                .quietHoursEnd(dataObject.getQuietHoursEnd())
                .subscribedBizTypesJson(dataObject.getSubscribedBizTypes())
                .createdAt(dataObject.getCreatedAt())
                .updatedAt(dataObject.getUpdatedAt())
                .build();
    }
    
    public static UserPreferenceDO toDO(UserPreference domain) {
        if (domain == null) {
            return null;
        }
        UserPreferenceDO dataObject = new UserPreferenceDO();
        dataObject.setId(domain.getId());
        dataObject.setTenantId(domain.getTenantId());
        dataObject.setUserId(domain.getUserId());
        dataObject.setChannelPreferences(domain.getChannelPreferencesJson());
        dataObject.setQuietHoursEnabled(domain.getQuietHoursEnabled());
        dataObject.setQuietHoursStart(domain.getQuietHoursStart());
        dataObject.setQuietHoursEnd(domain.getQuietHoursEnd());
        dataObject.setSubscribedBizTypes(domain.getSubscribedBizTypesJson());
        dataObject.setCreatedAt(domain.getCreatedAt());
        dataObject.setUpdatedAt(domain.getUpdatedAt());
        return dataObject;
    }
}
