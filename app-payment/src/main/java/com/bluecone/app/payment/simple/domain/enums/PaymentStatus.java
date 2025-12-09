package com.bluecone.app.payment.simple.domain.enums;

/**
 * Mini 支付流程的状态枚举，简化版仅覆盖本地调试场景。
 */
public enum PaymentStatus {

    WAIT_PAY("WAIT_PAY", "待支付"),
    SUCCESS("SUCCESS", "支付成功"),
    FAILED("FAILED", "支付失败"),
    CLOSED("CLOSED", "已关闭");

    private final String code;
    private final String desc;

    PaymentStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isFinal() {
        return this == SUCCESS || this == FAILED || this == CLOSED;
    }
}
