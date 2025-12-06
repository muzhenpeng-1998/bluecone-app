package com.bluecone.app.payment.domain.enums;

import java.util.Arrays;

/**
 * 支付方式枚举，表示具体的渠道能力/接入形态。
 */
public enum PaymentMethod {

    WECHAT_JSAPI("WECHAT_JSAPI", "微信 JSAPI/小程序/公众号内支付"),
    WECHAT_NATIVE("WECHAT_NATIVE", "微信 Native 扫码支付"),
    WECHAT_APP("WECHAT_APP", "微信 App 支付"),
    WECHAT_H5("WECHAT_H5", "微信 H5 支付"),
    ALIPAY_APP("ALIPAY_APP", "支付宝 App 支付"),
    ALIPAY_PAGE("ALIPAY_PAGE", "支付宝当面付/网页收银台"),
    BALANCE_DIRECT("BALANCE_DIRECT", "余额直接扣款"),
    CASH_OFFLINE("CASH_OFFLINE", "线下现金"),
    CARD_POS("CARD_POS", "线下 POS 刷卡");

    private final String code;
    private final String desc;

    PaymentMethod(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static PaymentMethod fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
