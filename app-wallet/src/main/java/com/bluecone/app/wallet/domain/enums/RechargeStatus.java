package com.bluecone.app.wallet.domain.enums;

/**
 * 充值单状态枚举
 * 
 * @author bluecone
 * @since 2025-12-19
 */
public enum RechargeStatus {
    
    /**
     * 初始化（充值单已创建，等待拉起支付）
     */
    INIT("INIT", "初始化"),
    
    /**
     * 支付中（已拉起支付，等待支付回调）
     */
    PAYING("PAYING", "支付中"),
    
    /**
     * 已支付（支付成功，已入账）
     */
    PAID("PAID", "已支付"),
    
    /**
     * 已关闭（超时关闭或用户取消）
     */
    CLOSED("CLOSED", "已关闭");
    
    private final String code;
    private final String description;
    
    RechargeStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static RechargeStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (RechargeStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * 是否为终态（不可再变更）
     */
    public boolean isFinalState() {
        return this == PAID || this == CLOSED;
    }
    
    /**
     * 是否可以流转到目标状态
     */
    public boolean canTransitionTo(RechargeStatus target) {
        if (this == target) {
            return true; // 幂等：相同状态可重复设置
        }
        
        if (this.isFinalState()) {
            return false; // 终态不可变更
        }
        
        // 状态流转规则
        switch (this) {
            case INIT:
                return target == PAYING || target == CLOSED;
            case PAYING:
                return target == PAID || target == CLOSED;
            default:
                return false;
        }
    }
}
