package com.bluecone.app.order.domain.enums;

import java.util.Arrays;

/**
 * 订单主状态。
 */
public enum OrderStatus {

    INIT("INIT", "初始化"),
    WAIT_PAY("WAIT_PAY", "待支付"),
    PAID("PAID", "已支付"),
    WAIT_ACCEPT("WAIT_ACCEPT", "待接单"),
    ACCEPTED("ACCEPTED", "已接单"),
    CANCELED("CANCELED", "已取消"),

    DRAFT("DRAFT", "草稿/预下单"),
    LOCKED_FOR_CHECKOUT("LOCKED_FOR_CHECKOUT", "草稿锁定"),
    PENDING_CONFIRM("PENDING_CONFIRM", "待确认"),
    PENDING_PAYMENT("PENDING_PAYMENT", "待支付"),
    PENDING_ACCEPT("PENDING_ACCEPT", "待接单"),
    IN_PROGRESS("IN_PROGRESS", "制作中/服务中"),
    READY("READY", "已出餐/待取货"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消"),
    REFUNDED("REFUNDED", "已退款"),
    CLOSED("CLOSED", "已关闭");

    private final String code;
    private final String desc;

    OrderStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static OrderStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断订单是否处于允许商户接单的状态。
     */
    public boolean canAccept() {
        return this == WAIT_ACCEPT;
    }
}
