package com.bluecone.app.payment.domain.enums;

import java.util.Arrays;

/**
 * 支付单状态枚举，描述资金侧的主状态，语义区别于订单状态。
 */
public enum PaymentStatus {

    INIT("INIT", "初始化，尚未发起三方支付"),
    PENDING("PENDING", "待支付（已向渠道下单，等待用户操作）"),
    SUCCESS("SUCCESS", "支付成功"),
    FAILED("FAILED", "支付失败，可重试"),
    CANCELED("CANCELED", "已取消（用户/商户/系统取消或超时关闭）"),
    REFUNDING("REFUNDING", "退款进行中"),
    REFUNDED("REFUNDED", "已退款完成"),
    CLOSED("CLOSED", "已关闭（风控/对账等原因彻底终止）");

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

    /**
     * 根据编码解析状态，空值返回 null，忽略大小写。
     */
    public static PaymentStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
