package com.bluecone.app.infra.integration.domain;

/**
 * Integration Hub 支持的通道枚举。
 *
 * <p>code 作为数据库存储值与外部配置标识，保持稳定不变。</p>
 */
public enum IntegrationChannelType {

    /**
     * 通用 HTTP Webhook。
     */
    WEBHOOK("WEBHOOK", "HTTP webhook 回调"),

    /**
     * 企业微信/微信群机器人。
     */
    WECHAT_BOT("WECHAT_BOT", "WeChat 机器人"),

    /**
     * 内部 HTTP 调用（预留）。
     */
    INTERNAL_HTTP("INTERNAL_HTTP", "内部 HTTP 通道"),

    /**
     * 预留 Kafka 等流式通道。
     */
    FUTURE_KAFKA("FUTURE_KAFKA", "预留 Kafka/消息总线");

    private final String code;
    private final String description;

    IntegrationChannelType(final String code, final String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
