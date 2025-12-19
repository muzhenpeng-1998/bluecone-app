package com.bluecone.app.notify.domain.model;

import com.bluecone.app.notify.api.enums.NotificationChannel;
import com.bluecone.app.notify.api.enums.TemplateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知模板领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyTemplate {
    
    private Long id;
    private Long tenantId;
    private String templateCode;
    private String templateName;
    private String bizType;
    private NotificationChannel channel;
    private String titleTemplate;
    private String contentTemplate;
    private String templateVariablesJson;
    private TemplateStatus status;
    private Integer priority;
    private String rateLimitConfigJson;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 是否启用
     */
    public boolean isEnabled() {
        return status == TemplateStatus.ENABLED;
    }
    
    /**
     * 渲染标题
     */
    public String renderTitle(Map<String, Object> variables) {
        if (titleTemplate == null) {
            return null;
        }
        return renderTemplate(titleTemplate, variables);
    }
    
    /**
     * 渲染内容
     */
    public String renderContent(Map<String, Object> variables) {
        return renderTemplate(contentTemplate, variables);
    }
    
    /**
     * 简单模板渲染：替换 {{varName}} 占位符
     */
    private String renderTemplate(String template, Map<String, Object> variables) {
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }
}
