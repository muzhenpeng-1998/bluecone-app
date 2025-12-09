package com.bluecone.app.application.gateway.dto.webhook;

import lombok.Data;

/**
 * 更新 webhook 配置的请求 DTO。
 */
@Data
public class WebhookConfigUpdateRequest {

    private Long id;

    private String targetUrl;

    /**
     * 可选：null 表示不修改，空串表示清空。
     */
    private String secret;

    /**
     * 1 启用 / 0 禁用。
     */
    private Integer enabled;

    private String description;
}
