package com.bluecone.app.campaign.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 更新活动命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignUpdateCommand implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 租户ID
     */
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;
    
    /**
     * 活动ID
     */
    @NotNull(message = "活动ID不能为空")
    private Long campaignId;
    
    /**
     * 活动名称
     */
    private String campaignName;
    
    /**
     * 活动规则（JSON 格式）
     */
    private CampaignRulesDTO rules;
    
    /**
     * 活动范围（JSON 格式）
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
     * 操作人ID
     */
    private Long operatorId;
    
    /**
     * 操作人名称
     */
    private String operatorName;
}
