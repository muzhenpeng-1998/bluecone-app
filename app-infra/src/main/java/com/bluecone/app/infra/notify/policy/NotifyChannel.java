package com.bluecone.app.infra.notify.policy;

/**
 * 通道枚举（Delivery 层标识）。
 *
 * <p>WeChat 机器人作为其中一个实现，后续可扩展短信、邮件、小程序等。</p>
 */
public enum NotifyChannel {

    WECHAT_BOT("wechat_bot"),

    MINIAPP_TEMPLATE("miniapp_template"),

    SMS("sms"),

    EMAIL("email"),

    IN_APP("in_app"),

    GENERIC_WEBHOOK("generic_webhook");

    private final String code;

    NotifyChannel(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
