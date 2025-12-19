package com.bluecone.app.billing.domain.enums;

/**
 * 提醒任务状态枚举
 */
public enum ReminderStatus {
    
    /**
     * 待发送
     */
    PENDING("PENDING", "待发送"),
    
    /**
     * 已发送
     */
    SENT("SENT", "已发送"),
    
    /**
     * 发送失败（重试耗尽）
     */
    FAILED("FAILED", "发送失败");
    
    private final String code;
    private final String name;
    
    ReminderStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public static ReminderStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ReminderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
    
    public boolean isPending() {
        return this == PENDING;
    }
    
    public boolean isSent() {
        return this == SENT;
    }
    
    public boolean isFailed() {
        return this == FAILED;
    }
}
