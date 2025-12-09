package com.bluecone.app.application.gateway.dto.webhook;

import lombok.Data;

/**
 * 测试 webhook 配置的结果。
 */
@Data
public class WebhookConfigTestResult {

    private boolean success;
    private Integer httpStatus;
    private String responseBody;
    private String errorMessage;
}
