package com.bluecone.app.notify.dto.webhook;

import java.util.Map;
import lombok.Data;

/**
 * Webhook 配置测试请求。
 */
@Data
public class WebhookConfigTestRequest {

    private Long id;

    /**
     * 可选：自定义测试数据。
     */
    private Map<String, Object> testData;
}
