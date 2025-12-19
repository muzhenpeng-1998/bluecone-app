package com.bluecone.app.wallet.domain.enums;

/**
 * 冻结状态枚举
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public enum FreezeStatus {
    
    /**
     * 已冻结（等待提交或释放）
     */
    FROZEN("FROZEN", "已冻结"),
    
    /**
     * 已提交（支付成功，余额已扣除）
     */
    COMMITTED("COMMITTED", "已提交"),
    
    /**
     * 已释放（取消订单，余额已恢复）
     */
    RELEASED("RELEASED", "已释放"),
    
    /**
     * 已回退（退款，余额已返还）
     */
    REVERTED("REVERTED", "已回退");
    
    private final String code;
    private final String description;
    
    FreezeStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static FreezeStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (FreezeStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
