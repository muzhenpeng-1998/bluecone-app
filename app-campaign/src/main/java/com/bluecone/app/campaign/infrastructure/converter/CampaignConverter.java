package com.bluecone.app.campaign.infrastructure.converter;

import com.bluecone.app.campaign.api.dto.CampaignDTO;
import com.bluecone.app.campaign.api.dto.CampaignRulesDTO;
import com.bluecone.app.campaign.api.dto.CampaignScopeDTO;
import com.bluecone.app.campaign.api.dto.ExecutionLogDTO;
import com.bluecone.app.campaign.api.enums.CampaignStatus;
import com.bluecone.app.campaign.api.enums.CampaignType;
import com.bluecone.app.campaign.api.enums.ExecutionStatus;
import com.bluecone.app.campaign.domain.model.Campaign;
import com.bluecone.app.campaign.domain.model.ExecutionLog;
import com.bluecone.app.campaign.infrastructure.persistence.po.CampaignPO;
import com.bluecone.app.campaign.infrastructure.persistence.po.ExecutionLogPO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 活动转换器
 */
@Slf4j
@Component
public class CampaignConverter {
    
    private final ObjectMapper objectMapper;

    public CampaignConverter(@Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * PO -> Domain
     */
    public Campaign toDomain(CampaignPO po) {
        if (po == null) {
            return null;
        }
        
        CampaignRulesDTO rules = parseJson(po.getRulesJson(), CampaignRulesDTO.class);
        CampaignScopeDTO scope = parseJson(po.getScopeJson(), CampaignScopeDTO.class);
        
        return Campaign.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .campaignCode(po.getCampaignCode())
                .campaignName(po.getCampaignName())
                .campaignType(CampaignType.valueOf(po.getCampaignType()))
                .status(CampaignStatus.valueOf(po.getStatus()))
                .rules(rules)
                .scope(scope)
                .startTime(po.getStartTime())
                .endTime(po.getEndTime())
                .priority(po.getPriority())
                .description(po.getDescription())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
    
    /**
     * Domain -> PO
     */
    public CampaignPO toPO(Campaign domain) {
        if (domain == null) {
            return null;
        }
        
        String rulesJson = toJson(domain.getRules());
        String scopeJson = toJson(domain.getScope());
        
        return CampaignPO.builder()
                .id(domain.getId())
                .tenantId(domain.getTenantId())
                .campaignCode(domain.getCampaignCode())
                .campaignName(domain.getCampaignName())
                .campaignType(domain.getCampaignType().name())
                .status(domain.getStatus().name())
                .rulesJson(rulesJson)
                .scopeJson(scopeJson)
                .startTime(domain.getStartTime())
                .endTime(domain.getEndTime())
                .priority(domain.getPriority())
                .description(domain.getDescription())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
    
    /**
     * Domain -> DTO
     */
    public CampaignDTO toDTO(Campaign domain) {
        if (domain == null) {
            return null;
        }
        
        return CampaignDTO.builder()
                .id(domain.getId())
                .tenantId(domain.getTenantId())
                .campaignCode(domain.getCampaignCode())
                .campaignName(domain.getCampaignName())
                .campaignType(domain.getCampaignType())
                .status(domain.getStatus())
                .rules(domain.getRules())
                .scope(domain.getScope())
                .startTime(domain.getStartTime())
                .endTime(domain.getEndTime())
                .priority(domain.getPriority())
                .description(domain.getDescription())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
    
    /**
     * ExecutionLog PO -> Domain
     */
    public ExecutionLog toExecutionLogDomain(ExecutionLogPO po) {
        if (po == null) {
            return null;
        }
        
        return ExecutionLog.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .campaignId(po.getCampaignId())
                .campaignCode(po.getCampaignCode())
                .campaignType(CampaignType.valueOf(po.getCampaignType()))
                .idempotencyKey(po.getIdempotencyKey())
                .userId(po.getUserId())
                .bizOrderId(po.getBizOrderId())
                .bizOrderNo(po.getBizOrderNo())
                .bizAmount(po.getBizAmount())
                .executionStatus(ExecutionStatus.valueOf(po.getExecutionStatus()))
                .rewardAmount(po.getRewardAmount())
                .rewardResultId(po.getRewardResultId())
                .failureReason(po.getFailureReason())
                .executedAt(po.getExecutedAt())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
    
    /**
     * ExecutionLog Domain -> PO
     */
    public ExecutionLogPO toExecutionLogPO(ExecutionLog domain) {
        if (domain == null) {
            return null;
        }
        
        return ExecutionLogPO.builder()
                .id(domain.getId())
                .tenantId(domain.getTenantId())
                .campaignId(domain.getCampaignId())
                .campaignCode(domain.getCampaignCode())
                .campaignType(domain.getCampaignType().name())
                .idempotencyKey(domain.getIdempotencyKey())
                .userId(domain.getUserId())
                .bizOrderId(domain.getBizOrderId())
                .bizOrderNo(domain.getBizOrderNo())
                .bizAmount(domain.getBizAmount())
                .executionStatus(domain.getExecutionStatus().name())
                .rewardAmount(domain.getRewardAmount())
                .rewardResultId(domain.getRewardResultId())
                .failureReason(domain.getFailureReason())
                .executedAt(domain.getExecutedAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
    
    /**
     * ExecutionLog Domain -> DTO
     */
    public ExecutionLogDTO toExecutionLogDTO(ExecutionLog domain) {
        if (domain == null) {
            return null;
        }
        
        return ExecutionLogDTO.builder()
                .id(domain.getId())
                .tenantId(domain.getTenantId())
                .campaignId(domain.getCampaignId())
                .campaignCode(domain.getCampaignCode())
                .campaignType(domain.getCampaignType())
                .idempotencyKey(domain.getIdempotencyKey())
                .userId(domain.getUserId())
                .bizOrderId(domain.getBizOrderId())
                .bizOrderNo(domain.getBizOrderNo())
                .bizAmount(domain.getBizAmount())
                .executionStatus(domain.getExecutionStatus())
                .rewardAmount(domain.getRewardAmount())
                .rewardResultId(domain.getRewardResultId())
                .failureReason(domain.getFailureReason())
                .executedAt(domain.getExecutedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
    
    private <T> T parseJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON: {}", json, e);
            return null;
        }
    }
    
    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize to JSON: {}", obj, e);
            return null;
        }
    }
}
