package com.bluecone.app.notify.api.dto;

import com.bluecone.app.notify.api.enums.NotificationChannel;
import com.bluecone.app.notify.api.enums.TemplateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 模板DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateDTO {
    
    private Long id;
    private Long tenantId;
    private String templateCode;
    private String templateName;
    private String bizType;
    private NotificationChannel channel;
    private String titleTemplate;
    private String contentTemplate;
    private List<CreateTemplateRequest.TemplateVariable> templateVariables;
    private TemplateStatus status;
    private Integer priority;
    private Map<String, Object> rateLimitConfig;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
