package com.bluecone.app.billing.domain.enums;

/**
 * 通知渠道枚举
 */
public enum NotificationChannel {
    
    /**
     * 站内通知
     */
    IN_APP("IN_APP", "站内通知"),
    
    /**
     * 邮件通知
     */
    EMAIL("EMAIL", "邮件通知"),
    
    /**
     * 短信通知
     */
    SMS("SMS", "短信通知");
    
    private final String code;
    private final String name;
    
    NotificationChannel(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public static NotificationChannel fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (NotificationChannel channel : values()) {
            if (channel.code.equals(code)) {
                return channel;
            }
        }
        return null;
    }
}
