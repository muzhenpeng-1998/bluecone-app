package com.bluecone.app.notify.api.dto;

import com.bluecone.app.notify.api.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 创建模板请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTemplateRequest {
    
    /**
     * 租户ID（NULL=系统级模板）
     */
    private Long tenantId;
    
    /**
     * 模板编码
     */
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;
    
    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空")
    private String templateName;
    
    /**
     * 业务类型
     */
    @NotBlank(message = "业务类型不能为空")
    private String bizType;
    
    /**
     * 通知渠道
     */
    @NotNull(message = "通知渠道不能为空")
    private NotificationChannel channel;
    
    /**
     * 标题模板（站内信/邮件主题）
     */
    private String titleTemplate;
    
    /**
     * 内容模板
     */
    @NotBlank(message = "内容模板不能为空")
    private String contentTemplate;
    
    /**
     * 模板变量定义
     */
    private List<TemplateVariable> templateVariables;
    
    /**
     * 优先级（默认50）
     */
    private Integer priority;
    
    /**
     * 频控配置（可选）
     */
    private Map<String, Object> rateLimitConfig;
    
    /**
     * 创建人
     */
    private String createdBy;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateVariable {
        private String name;
        private String type;
        private String description;
    }
}
