package com.bluecone.app.payment.simple.domain.enums;

/**
 * 简化支付渠道枚举，仅为小程序调试支付提供。
 */
public enum PaymentChannel {

    WECHAT_MINI("WECHAT_MINI", "微信小程序"),
    ALIPAY_MINI("ALIPAY_MINI", "支付宝小程序"),
    INTERNAL_DEBUG("INTERNAL_DEBUG", "内部调试模拟支付");

    private final String code;
    private final String desc;

    PaymentChannel(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
