package com.bluecone.app.growth.infrastructure.converter;

import com.bluecone.app.growth.api.enums.AttributionStatus;
import com.bluecone.app.growth.api.enums.CampaignStatus;
import com.bluecone.app.growth.api.enums.CampaignType;
import com.bluecone.app.growth.api.enums.IssueStatus;
import com.bluecone.app.growth.api.enums.RewardType;
import com.bluecone.app.growth.api.enums.UserRole;
import com.bluecone.app.growth.domain.model.Attribution;
import com.bluecone.app.growth.domain.model.GrowthCampaign;
import com.bluecone.app.growth.domain.model.InviteCode;
import com.bluecone.app.growth.domain.model.RewardIssueLog;
import com.bluecone.app.growth.infrastructure.persistence.po.AttributionPO;
import com.bluecone.app.growth.infrastructure.persistence.po.GrowthCampaignPO;
import com.bluecone.app.growth.infrastructure.persistence.po.InviteCodePO;
import com.bluecone.app.growth.infrastructure.persistence.po.RewardIssueLogPO;

/**
 * 增长模块转换器
 */
public class GrowthConverter {
    
    public static GrowthCampaign toDomain(GrowthCampaignPO po) {
        if (po == null) {
            return null;
        }
        return GrowthCampaign.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .campaignCode(po.getCampaignCode())
                .campaignName(po.getCampaignName())
                .campaignType(CampaignType.valueOf(po.getCampaignType()))
                .status(CampaignStatus.valueOf(po.getStatus()))
                .rulesJson(po.getRulesJson())
                .startTime(po.getStartTime())
                .endTime(po.getEndTime())
                .description(po.getDescription())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
    
    public static GrowthCampaignPO toPO(GrowthCampaign domain) {
        if (domain == null) {
            return null;
        }
        GrowthCampaignPO po = new GrowthCampaignPO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId());
        po.setCampaignCode(domain.getCampaignCode());
        po.setCampaignName(domain.getCampaignName());
        po.setCampaignType(domain.getCampaignType().name());
        po.setStatus(domain.getStatus().name());
        po.setRulesJson(domain.getRulesJson());
        po.setStartTime(domain.getStartTime());
        po.setEndTime(domain.getEndTime());
        po.setDescription(domain.getDescription());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }
    
    public static InviteCode toDomain(InviteCodePO po) {
        if (po == null) {
            return null;
        }
        return InviteCode.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .campaignCode(po.getCampaignCode())
                .inviteCode(po.getInviteCode())
                .inviterUserId(po.getInviterUserId())
                .invitesCount(po.getInvitesCount())
                .successfulInvitesCount(po.getSuccessfulInvitesCount())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
    
    public static InviteCodePO toPO(InviteCode domain) {
        if (domain == null) {
            return null;
        }
        InviteCodePO po = new InviteCodePO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId());
        po.setCampaignCode(domain.getCampaignCode());
        po.setInviteCode(domain.getInviteCode());
        po.setInviterUserId(domain.getInviterUserId());
        po.setInvitesCount(domain.getInvitesCount());
        po.setSuccessfulInvitesCount(domain.getSuccessfulInvitesCount());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }
    
    public static Attribution toDomain(AttributionPO po) {
        if (po == null) {
            return null;
        }
        return Attribution.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .campaignCode(po.getCampaignCode())
                .inviteCode(po.getInviteCode())
                .inviterUserId(po.getInviterUserId())
                .inviteeUserId(po.getInviteeUserId())
                .status(AttributionStatus.valueOf(po.getStatus()))
                .boundAt(po.getBoundAt())
                .confirmedAt(po.getConfirmedAt())
                .firstOrderId(po.getFirstOrderId())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
    
    public static AttributionPO toPO(Attribution domain) {
        if (domain == null) {
            return null;
        }
        AttributionPO po = new AttributionPO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId());
        po.setCampaignCode(domain.getCampaignCode());
        po.setInviteCode(domain.getInviteCode());
        po.setInviterUserId(domain.getInviterUserId());
        po.setInviteeUserId(domain.getInviteeUserId());
        po.setStatus(domain.getStatus().name());
        po.setBoundAt(domain.getBoundAt());
        po.setConfirmedAt(domain.getConfirmedAt());
        po.setFirstOrderId(domain.getFirstOrderId());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }
    
    public static RewardIssueLog toDomain(RewardIssueLogPO po) {
        if (po == null) {
            return null;
        }
        return RewardIssueLog.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .campaignCode(po.getCampaignCode())
                .idempotencyKey(po.getIdempotencyKey())
                .attributionId(po.getAttributionId())
                .userId(po.getUserId())
                .userRole(UserRole.valueOf(po.getUserRole()))
                .rewardType(RewardType.valueOf(po.getRewardType()))
                .rewardValue(po.getRewardValue())
                .issueStatus(IssueStatus.valueOf(po.getIssueStatus()))
                .resultId(po.getResultId())
                .errorCode(po.getErrorCode())
                .errorMessage(po.getErrorMessage())
                .triggerOrderId(po.getTriggerOrderId())
                .issuedAt(po.getIssuedAt())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
    
    public static RewardIssueLogPO toPO(RewardIssueLog domain) {
        if (domain == null) {
            return null;
        }
        RewardIssueLogPO po = new RewardIssueLogPO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId());
        po.setCampaignCode(domain.getCampaignCode());
        po.setIdempotencyKey(domain.getIdempotencyKey());
        po.setAttributionId(domain.getAttributionId());
        po.setUserId(domain.getUserId());
        po.setUserRole(domain.getUserRole().name());
        po.setRewardType(domain.getRewardType().name());
        po.setRewardValue(domain.getRewardValue());
        po.setIssueStatus(domain.getIssueStatus().name());
        po.setResultId(domain.getResultId());
        po.setErrorCode(domain.getErrorCode());
        po.setErrorMessage(domain.getErrorMessage());
        po.setTriggerOrderId(domain.getTriggerOrderId());
        po.setIssuedAt(domain.getIssuedAt());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }
}
