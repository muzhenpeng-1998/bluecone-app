package com.bluecone.app.billing.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Dunning 发送日志实体
 */
@Data
@TableName("bc_billing_dunning_log")
public class BillingDunningLogDO {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long reminderTaskId;
    private Long subscriptionId;
    private Long tenantId;
    
    private String reminderType;
    private String notificationChannel;
    private String recipient;
    
    private String sendResult;
    private String errorMessage;
    private String responseData;
    
    private LocalDateTime createdAt;
}
