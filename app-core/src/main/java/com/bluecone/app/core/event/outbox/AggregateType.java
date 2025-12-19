package com.bluecone.app.core.event.outbox;

/**
 * 聚合根类型枚举
 * 定义系统中所有的聚合根类型
 */
public enum AggregateType {
    
    /**
     * 订单聚合根
     */
    ORDER("ORDER"),
    
    /**
     * 支付单聚合根
     */
    PAYMENT("PAYMENT"),
    
    /**
     * 退款单聚合根
     */
    REFUND("REFUND"),
    
    /**
     * 优惠券聚合根
     */
    COUPON("COUPON"),
    
    /**
     * 钱包聚合根
     */
    WALLET("WALLET"),
    
    /**
     * 积分聚合根
     */
    POINTS("POINTS");
    
    private final String code;
    
    AggregateType(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    /**
     * 从 code 解析 AggregateType
     */
    public static AggregateType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AggregateType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return code;
    }
}
