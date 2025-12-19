package com.bluecone.app.billing.domain.enums;

/**
 * 订阅状态枚举
 */
public enum SubscriptionStatus {
    
    /**
     * 生效中
     */
    ACTIVE("ACTIVE", "生效中"),
    
    /**
     * 宽限期（到期后进入宽限期，限制部分功能但不立即降级）
     */
    GRACE("GRACE", "宽限期"),
    
    /**
     * 已过期
     */
    EXPIRED("EXPIRED", "已过期"),
    
    /**
     * 已取消
     */
    CANCELED("CANCELED", "已取消");
    
    private final String code;
    private final String name;
    
    SubscriptionStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public static SubscriptionStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (SubscriptionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
    
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    public boolean isGrace() {
        return this == GRACE;
    }
    
    public boolean isExpired() {
        return this == EXPIRED;
    }
    
    /**
     * 是否可以使用基本功能（ACTIVE 或 GRACE）
     */
    public boolean canUseBasicFeatures() {
        return this == ACTIVE || this == GRACE;
    }
}
