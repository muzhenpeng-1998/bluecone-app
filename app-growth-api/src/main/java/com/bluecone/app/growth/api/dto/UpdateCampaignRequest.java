package com.bluecone.app.growth.api.dto;

import com.bluecone.app.growth.api.enums.CampaignStatus;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 更新活动请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCampaignRequest {
    
    /**
     * 活动名称
     */
    private String campaignName;
    
    /**
     * 活动状态
     */
    private CampaignStatus status;
    
    /**
     * 奖励规则
     */
    @Valid
    private CampaignRules rules;
    
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
}
