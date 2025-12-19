package com.bluecone.app.campaign.api.enums;

/**
 * 活动执行状态枚举
 */
public enum ExecutionStatus {
    
    /**
     * 执行成功
     */
    SUCCESS("执行成功"),
    
    /**
     * 执行失败
     */
    FAILED("执行失败"),
    
    /**
     * 条件不满足（跳过）
     */
    SKIPPED("条件不满足");
    
    private final String description;
    
    ExecutionStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
