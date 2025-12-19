package com.bluecone.app.notify.api.enums;

/**
 * 通知任务状态枚举
 */
public enum NotificationTaskStatus {
    /**
     * 待发送
     */
    PENDING,
    
    /**
     * 发送中
     */
    SENDING,
    
    /**
     * 已发送
     */
    SENT,
    
    /**
     * 发送失败
     */
    FAILED,
    
    /**
     * 被频控限制
     */
    RATE_LIMITED,
    
    /**
     * 已取消
     */
    CANCELLED
}
