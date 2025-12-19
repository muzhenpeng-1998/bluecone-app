package com.bluecone.app.wallet.domain.enums;

/**
 * 账户状态枚举
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public enum AccountStatus {
    
    /**
     * 活跃（正常使用）
     */
    ACTIVE("ACTIVE", "活跃"),
    
    /**
     * 冻结（账户被冻结，不可操作）
     */
    FROZEN("FROZEN", "冻结"),
    
    /**
     * 关闭（账户已关闭）
     */
    CLOSED("CLOSED", "关闭");
    
    private final String code;
    private final String description;
    
    AccountStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static AccountStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AccountStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
