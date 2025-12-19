package com.bluecone.app.campaign.api.dto;

import com.bluecone.app.campaign.api.enums.CampaignStatus;
import com.bluecone.app.campaign.api.enums.CampaignType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
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
     * 活动规则
     */
    private CampaignRulesDTO rules;
    
    /**
     * 活动范围
     */
    private CampaignScopeDTO scope;
    
    /**
     * 活动开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 活动结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 优先级
     */
    private Integer priority;
    
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
}
