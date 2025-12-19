package com.bluecone.app.notify.infrastructure.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知模板数据对象
 */
@Data
@TableName("bc_notify_template")
public class NotifyTemplateDO {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long tenantId;
    private String templateCode;
    private String templateName;
    private String bizType;
    private String channel;
    private String titleTemplate;
    private String contentTemplate;
    private String templateVariables;
    private String status;
    private Integer priority;
    private String rateLimitConfig;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
