package com.bluecone.app.application.gateway.dto.webhook;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * Webhook 配置对外展示 DTO。
 */
@Data
public class WebhookConfigView {

    private Long id;
    private Long tenantId;
    private String eventType;
    private String targetUrl;
    private Integer enabled;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 仅用于提示是否配置过密钥，不返回密钥原文。
     */
    private boolean secretConfigured;
}
