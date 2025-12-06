package com.bluecone.app.payment.domain.enums;

import java.util.Arrays;

/**
 * 支付状态事件，表示驱动支付状态变化的动作。
 * <p>
 * 触发方可能包括：用户（取消）、商户后台（关闭）、系统定时任务（超时）、支付渠道回调（成功/失败）、风控或对账流程（关闭）。
 */
public enum PaymentEvent {

    /**
     * 发起支付：从 INIT 进入 PENDING，或从 FAILED 重新发起。
     */
    INITIATE("INITIATE", "发起支付"),
    /**
     * 支付成功：通常由渠道异步回调或轮询结果触发。
     */
    PAY_SUCCESS("PAY_SUCCESS", "支付成功"),
    /**
     * 支付失败：渠道返回失败、关单、或用户支付失败。
     */
    PAY_FAILED("PAY_FAILED", "支付失败"),
    /**
     * 用户主动取消支付。
     */
    USER_CANCEL("USER_CANCEL", "用户取消"),
    /**
     * 商户/门店后台取消支付。
     */
    MERCHANT_CANCEL("MERCHANT_CANCEL", "商户取消"),
    /**
     * 支付超时：长时间未支付自动关单。
     */
    PAY_TIMEOUT("PAY_TIMEOUT", "支付超时"),
    /**
     * 申请退款：从 SUCCESS 进入 REFUNDING。
     */
    APPLY_REFUND("APPLY_REFUND", "申请退款"),
    /**
     * 退款成功：进入 REFUNDED。
     */
    REFUND_SUCCESS("REFUND_SUCCESS", "退款成功"),
    /**
     * 退款失败：维持原支付成功状态，便于重试。
     */
    REFUND_FAILED("REFUND_FAILED", "退款失败"),
    /**
     * 风控/对账等直接关闭资金单：进入 CLOSED。
     */
    CLOSE("CLOSE", "直接关闭资金单");

    private final String code;
    private final String desc;

    PaymentEvent(String code, String desc) {
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
     * 根据编码解析事件，空值返回 null。
     */
    public static PaymentEvent fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
