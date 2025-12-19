package com.bluecone.app.campaign.domain.model;

import com.bluecone.app.campaign.api.dto.CampaignRulesDTO;
import com.bluecone.app.campaign.api.dto.CampaignScopeDTO;
import com.bluecone.app.campaign.api.enums.CampaignStatus;
import com.bluecone.app.campaign.api.enums.CampaignType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 活动领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {
    
    /**
     * 活动ID
     */
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 活动编码（租户内唯一）
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
     * 活动结束时间（null 表示长期有效）
     */
    private LocalDateTime endTime;
    
    /**
     * 优先级（数字越大优先级越高）
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
    
    /**
     * 检查活动是否有效（可执行）
     * 状态必须是 ONLINE，且在时间窗口内
     */
    public boolean isEffective() {
        if (status != CampaignStatus.ONLINE) {
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
     * 只有 DRAFT 和 OFFLINE 状态可以修改
     */
    public boolean canUpdate() {
        return status == CampaignStatus.DRAFT || status == CampaignStatus.OFFLINE;
    }
    
    /**
     * 上线活动
     */
    public void online() {
        if (status != CampaignStatus.DRAFT && status != CampaignStatus.OFFLINE) {
            throw new IllegalStateException("只有草稿或下线状态的活动可以上线");
        }
        this.status = CampaignStatus.ONLINE;
    }
    
    /**
     * 下线活动
     */
    public void offline() {
        if (status != CampaignStatus.ONLINE) {
            throw new IllegalStateException("只有上线状态的活动可以下线");
        }
        this.status = CampaignStatus.OFFLINE;
    }
}
