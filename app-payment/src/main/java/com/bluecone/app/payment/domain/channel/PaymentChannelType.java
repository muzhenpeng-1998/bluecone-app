package com.bluecone.app.payment.domain.channel;

import java.util.Arrays;

/**
 * 支付渠道类型枚举。
 */
public enum PaymentChannelType {

    WECHAT_JSAPI("WECHAT_JSAPI"),
    WECHAT_NATIVE("WECHAT_NATIVE"),
    ALIPAY_MINI("ALIPAY_MINI");

    private final String code;

    PaymentChannelType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 根据编码解析渠道类型。
     */
    public static PaymentChannelType fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的支付渠道类型: " + code));
    }
}
