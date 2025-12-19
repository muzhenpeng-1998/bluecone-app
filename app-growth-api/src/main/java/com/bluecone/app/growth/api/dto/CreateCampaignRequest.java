package com.bluecone.app.growth.api.dto;

import com.bluecone.app.growth.api.enums.CampaignType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 创建活动请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCampaignRequest {
    
    /**
     * 活动编码
     */
    @NotBlank(message = "活动编码不能为空")
    private String campaignCode;
    
    /**
     * 活动名称
     */
    @NotBlank(message = "活动名称不能为空")
    private String campaignName;
    
    /**
     * 活动类型
     */
    @NotNull(message = "活动类型不能为空")
    private CampaignType campaignType;
    
    /**
     * 奖励规则
     */
    @NotNull(message = "奖励规则不能为空")
    @Valid
    private CampaignRules rules;
    
    /**
     * 活动开始时间
     */
    @NotNull(message = "活动开始时间不能为空")
    private LocalDateTime startTime;
    
    /**
     * 活动结束时间（可选）
     */
    private LocalDateTime endTime;
    
    /**
     * 活动描述
     */
    private String description;
}
