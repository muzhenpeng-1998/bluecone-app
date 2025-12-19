package com.bluecone.app.notify.infrastructure.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知任务数据对象
 */
@Data
@TableName("bc_notify_task")
public class NotifyTaskDO {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long tenantId;
    private Long userId;
    private String bizType;
    private String bizId;
    private String templateCode;
    private String channel;
    private Integer priority;
    private String variables;
    private String title;
    private String content;
    private String idempotencyKey;
    private String status;
    private Integer retryCount;
    private Integer maxRetryCount;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private LocalDateTime rateLimitCheckedAt;
    private Boolean rateLimitPassed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime sentAt;
}
