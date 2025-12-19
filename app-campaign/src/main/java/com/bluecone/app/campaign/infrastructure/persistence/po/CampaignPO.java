package com.bluecone.app.campaign.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 活动持久化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("bc_campaign")
public class CampaignPO {
    
    /**
     * 活动ID
     */
    @TableId(type = IdType.INPUT)
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
    private String campaignType;
    
    /**
     * 活动状态
     */
    private String status;
    
    /**
     * 活动规则（JSON）
     */
    private String rulesJson;
    
    /**
     * 活动范围（JSON）
     */
    private String scopeJson;
    
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
