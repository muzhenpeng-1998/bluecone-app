package com.bluecone.app.order.domain.enums;

import java.util.Arrays;

/**
 * 支付状态。
 */
public enum PayStatus {

    INIT("INIT", "待支付初始化"),
    UNPAID("UNPAID", "未支付"),
    PAID("PAID", "已支付"),
    REFUNDING("REFUNDING", "退款中"),
    REFUNDED("REFUNDED", "已退款"),
    PARTIAL_REFUND("PARTIAL_REFUND", "部分退款"),
    FULL_REFUND("FULL_REFUND", "全额退款");

    private final String code;
    private final String desc;

    PayStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static PayStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
