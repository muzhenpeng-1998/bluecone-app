package com.bluecone.app.campaign.api.dto;

import com.bluecone.app.campaign.api.enums.CampaignType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 创建活动命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignCreateCommand implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 租户ID
     */
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;
    
    /**
     * 活动编码（唯一）
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
     * 活动规则（JSON 格式）
     */
    @NotNull(message = "活动规则不能为空")
    private CampaignRulesDTO rules;
    
    /**
     * 活动范围（JSON 格式）
     */
    @NotNull(message="活动范围不能为空")
    private CampaignScopeDTO scope;
    
    /**
     * 活动开始时间
     */
    @NotNull(message = "活动开始时间不能为空")
    private LocalDateTime startTime;
    
    /**
     * 活动结束时间（null 表示长期有效）
     */
    private LocalDateTime endTime;
    
    /**
     * 优先级（数字越大优先级越高，用于多活动排序）
     */
    @Builder.Default
    private Integer priority = 0;
    
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
