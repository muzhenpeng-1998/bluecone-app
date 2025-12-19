package com.bluecone.app.billing.notification;

import com.bluecone.app.billing.domain.enums.NotificationChannel;

/**
 * 通知发送器接口
 * 支持多渠道通知发送（站内通知、邮件、短信）
 */
public interface NotificationSender {
    
    /**
     * 发送提醒通知
     * 
     * @param channel 通知渠道
     * @param tenantId 租户ID
     * @param reminderType 提醒类型
     * @param planName 套餐名称
     * @param expireAt 到期时间
     * @param daysRemaining 剩余天数（负数表示已过期）
     * @return 发送结果
     */
    SendResult send(
        NotificationChannel channel,
        Long tenantId,
        String reminderType,
        String planName,
        String expireAt,
        int daysRemaining
    );
    
    /**
     * 发送结果
     */
    class SendResult {
        private final boolean success;
        private final String message;
        private final String responseData;
        
        public SendResult(boolean success, String message, String responseData) {
            this.success = success;
            this.message = message;
            this.responseData = responseData;
        }
        
        public static SendResult success(String message) {
            return new SendResult(true, message, null);
        }
        
        public static SendResult success(String message, String responseData) {
            return new SendResult(true, message, responseData);
        }
        
        public static SendResult failure(String message) {
            return new SendResult(false, message, null);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getResponseData() {
            return responseData;
        }
    }
}
