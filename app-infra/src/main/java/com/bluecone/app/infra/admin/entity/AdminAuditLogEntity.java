package com.bluecone.app.infra.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台审计日志实体
 */
@Data
@TableName("bc_admin_audit_log")
public class AdminAuditLogEntity {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("tenant_id")
    private Long tenantId;
    
    @TableField("operator_id")
    private Long operatorId;
    
    @TableField("operator_name")
    private String operatorName;
    
    @TableField("operator_ip")
    private String operatorIp;
    
    @TableField("action")
    private String action;
    
    @TableField("resource_type")
    private String resourceType;
    
    @TableField("resource_id")
    private Long resourceId;
    
    @TableField("resource_name")
    private String resourceName;
    
    @TableField("operation_desc")
    private String operationDesc;
    
    @TableField("request_uri")
    private String requestUri;
    
    @TableField("request_method")
    private String requestMethod;
    
    @TableField("data_before")
    private String dataBefore;
    
    @TableField("data_after")
    private String dataAfter;
    
    @TableField("change_summary")
    private String changeSummary;
    
    @TableField("status")
    private String status;
    
    @TableField("error_message")
    private String errorMessage;
    
    @TableField("trace_id")
    private String traceId;
    
    @TableField("user_agent")
    private String userAgent;
    
    @TableField("ext_json")
    private String extJson;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
}
