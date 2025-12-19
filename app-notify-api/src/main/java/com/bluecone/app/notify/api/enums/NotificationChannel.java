package com.bluecone.app.notify.api.enums;

/**
 * 通知渠道枚举
 */
public enum NotificationChannel {
    /**
     * 站内信
     */
    IN_APP("站内信"),
    
    /**
     * 邮件
     */
    EMAIL("邮件"),
    
    /**
     * 微信订阅消息
     */
    WECHAT("微信订阅消息"),
    
    /**
     * 短信
     */
    SMS("短信");
    
    private final String description;
    
    NotificationChannel(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
