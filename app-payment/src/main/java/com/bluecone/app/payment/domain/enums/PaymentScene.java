package com.bluecone.app.payment.domain.enums;

import java.util.Arrays;

/**
 * 支付场景枚举，用于多业态下扩展不同的支付状态机策略。
 */
public enum PaymentScene {

    DINE_IN("DINE_IN", "堂食"),
    TAKE_OUT("TAKE_OUT", "外卖/自提"),
    RETAIL("RETAIL", "零售"),
    MEMBERSHIP("MEMBERSHIP", "会员充值/年费"),
    SUBSCRIPTION("SUBSCRIPTION", "订阅"),
    OTHER("OTHER", "其他场景");

    private final String code;
    private final String desc;

    PaymentScene(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static PaymentScene fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
