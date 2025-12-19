package com.bluecone.app.growth.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 归因关系表PO
 */
@Data
@TableName("bc_growth_attribution")
public class AttributionPO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    
    private Long tenantId;
    
    private String campaignCode;
    
    private String inviteCode;
    
    private Long inviterUserId;
    
    private Long inviteeUserId;
    
    private String status;
    
    private LocalDateTime boundAt;
    
    private LocalDateTime confirmedAt;
    
    private Long firstOrderId;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
