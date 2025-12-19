package com.bluecone.app.billing.domain.enums;

/**
 * 账单状态枚举
 */
public enum InvoiceStatus {
    
    /**
     * 待支付
     */
    PENDING("PENDING", "待支付"),
    
    /**
     * 已支付
     */
    PAID("PAID", "已支付"),
    
    /**
     * 已过期（超时未支付）
     */
    EXPIRED("EXPIRED", "已过期"),
    
    /**
     * 已取消
     */
    CANCELED("CANCELED", "已取消");
    
    private final String code;
    private final String name;
    
    InvoiceStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public static InvoiceStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (InvoiceStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
    
    public boolean isPaid() {
        return this == PAID;
    }
    
    public boolean isPending() {
        return this == PENDING;
    }
}
