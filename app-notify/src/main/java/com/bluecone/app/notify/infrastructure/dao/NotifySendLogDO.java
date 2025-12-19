package com.bluecone.app.notify.infrastructure.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知发送日志数据对象
 */
@Data
@TableName("bc_notify_send_log")
public class NotifySendLogDO {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long taskId;
    private Long tenantId;
    private Long userId;
    private String channel;
    private String bizType;
    private String bizId;
    private String title;
    private String content;
    private String recipient;
    private String sendStatus;
    private String errorCode;
    private String errorMessage;
    private String providerResponse;
    private Integer sendDurationMs;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
