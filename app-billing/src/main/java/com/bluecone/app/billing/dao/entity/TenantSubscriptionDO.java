package com.bluecone.app.billing.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户订阅实体
 */
@Data
@TableName("bc_tenant_subscription")
public class TenantSubscriptionDO {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long tenantId;
    
    private String currentPlanCode;
    private String currentPlanName;
    private Integer currentPlanLevel;
    
    private String currentFeatures;
    
    private LocalDateTime subscriptionStartAt;
    private LocalDateTime subscriptionEndAt;
    
    private String status;
    
    private Long lastInvoiceId;
    private LocalDateTime lastPaidAt;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
