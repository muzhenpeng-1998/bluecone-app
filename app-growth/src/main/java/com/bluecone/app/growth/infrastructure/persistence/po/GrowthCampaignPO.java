package com.bluecone.app.growth.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 增长活动表PO
 */
@Data
@TableName("bc_growth_campaign")
public class GrowthCampaignPO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    
    private Long tenantId;
    
    private String campaignCode;
    
    private String campaignName;
    
    private String campaignType;
    
    private String status;
    
    private String rulesJson;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private String description;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
