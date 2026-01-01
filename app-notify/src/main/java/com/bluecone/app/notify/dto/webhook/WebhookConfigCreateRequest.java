package com.bluecone.app.notify.dto.webhook;

import lombok.Data;

/**
 * 新建 webhook 配置的请求 DTO。
 */
@Data
public class WebhookConfigCreateRequest {

    /**
     * 事件类型（不允许外部自定义租户 ID）。
     */
    private String eventType;

    /**
     * 回调地址。
     */
    private String targetUrl;

    /**
     * 签名密钥，允许为空由服务端生成。
     */
    private String secret;

    /**
     * 描述信息。
     */
    private String description;
}
