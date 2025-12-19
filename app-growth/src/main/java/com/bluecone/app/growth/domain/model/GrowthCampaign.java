package com.bluecone.app.growth.domain.model;

import com.bluecone.app.growth.api.dto.CampaignRules;
import com.bluecone.app.growth.api.enums.CampaignStatus;
import com.bluecone.app.growth.api.enums.CampaignType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 增长活动领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthCampaign {
    
    /**
     * 活动ID
     */
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 活动编码
     */
    private String campaignCode;
    
    /**
     * 活动名称
     */
    private String campaignName;
    
    /**
     * 活动类型
     */
    private CampaignType campaignType;
    
    /**
     * 活动状态
     */
    private CampaignStatus status;
    
    /**
     * 奖励规则（JSON格式）
     */
    private String rulesJson;
    
    /**
     * 活动开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 活动结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 活动描述
     */
    private String description;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 检查活动是否有效（可用于邀请）
     */
    public boolean isActive() {
        if (status != CampaignStatus.ACTIVE) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) {
            return false;
        }
        return endTime == null || now.isBefore(endTime);
    }
    
    /**
     * 检查活动是否可以修改
     */
    public boolean canUpdate() {
        return status == CampaignStatus.DRAFT || status == CampaignStatus.PAUSED;
    }
}
