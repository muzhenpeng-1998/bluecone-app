package com.bluecone.app.billing.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订阅到期提醒任务实体
 */
@Data
@TableName("bc_billing_reminder_task")
public class BillingReminderTaskDO {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long subscriptionId;
    private Long tenantId;
    
    private String reminderType;
    private LocalDateTime remindAt;
    private LocalDateTime expireAt;
    
    private String planCode;
    private String planName;
    
    private String status;
    private Integer retryCount;
    private Integer maxRetryCount;
    private LocalDateTime nextRetryAt;
    private String lastError;
    
    private LocalDateTime sentAt;
    private String notificationChannels;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
