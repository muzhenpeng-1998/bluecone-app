package com.bluecone.app.wallet.domain.enums;

/**
 * 业务类型枚举
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public enum BizType {
    
    /**
     * 充值
     */
    RECHARGE("RECHARGE", "充值"),
    
    /**
     * 订单支付
     */
    ORDER_PAY("ORDER_PAY", "订单支付"),
    
    /**
     * 退款
     */
    REFUND("REFUND", "退款"),
    
    /**
     * 管理员调整
     */
    ADJUST("ADJUST", "管理员调整"),
    
    /**
     * 订单下单（冻结）
     */
    ORDER_CHECKOUT("ORDER_CHECKOUT", "订单下单");
    
    private final String code;
    private final String description;
    
    BizType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static BizType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (BizType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
