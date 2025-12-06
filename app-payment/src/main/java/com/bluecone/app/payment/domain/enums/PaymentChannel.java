package com.bluecone.app.payment.domain.enums;

import java.util.Arrays;

/**
 * 支付渠道枚举，用于区分微信、支付宝、余额、现金等大渠道。
 */
public enum PaymentChannel {

    WECHAT("WECHAT", "微信支付"),
    ALIPAY("ALIPAY", "支付宝"),
    BALANCE("BALANCE", "平台余额"),
    CASH("CASH", "现金"),
    CARD("CARD", "刷卡/POS");

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

    public static PaymentChannel fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
