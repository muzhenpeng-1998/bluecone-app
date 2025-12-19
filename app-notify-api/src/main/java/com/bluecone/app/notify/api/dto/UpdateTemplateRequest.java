package com.bluecone.app.notify.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 更新模板请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTemplateRequest {
    
    /**
     * 模板名称
     */
    private String templateName;
    
    /**
     * 标题模板
     */
    private String titleTemplate;
    
    /**
     * 内容模板
     */
    private String contentTemplate;
    
    /**
     * 模板变量定义
     */
    private List<CreateTemplateRequest.TemplateVariable> templateVariables;
    
    /**
     * 优先级
     */
    private Integer priority;
    
    /**
     * 频控配置
     */
    private Map<String, Object> rateLimitConfig;
    
    /**
     * 更新人
     */
    private String updatedBy;
}
