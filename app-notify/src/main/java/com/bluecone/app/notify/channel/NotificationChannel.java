package com.bluecone.app.notify.channel;

import com.bluecone.app.notify.domain.model.NotifyTask;

/**
 * 通知渠道接口
 * 不同渠道（站内信/邮件/微信/短信）的统一抽象
 */
public interface NotificationChannel {
    
    /**
     * 获取渠道类型
     */
    com.bluecone.app.notify.api.enums.NotificationChannel getChannelType();
    
    /**
     * 发送通知
     * 
     * @param task 通知任务
     * @param recipient 接收方地址（邮箱/手机号/OpenID）
     * @return 发送结果
     */
    SendResult send(NotifyTask task, String recipient);
    
    /**
     * 发送结果
     */
    class SendResult {
        private final boolean success;
        private final String errorCode;
        private final String errorMessage;
        private final Integer durationMs;
        
        private SendResult(boolean success, String errorCode, String errorMessage, Integer durationMs) {
            this.success = success;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.durationMs = durationMs;
        }
        
        public static SendResult success(Integer durationMs) {
            return new SendResult(true, null, null, durationMs);
        }
        
        public static SendResult failure(String errorCode, String errorMessage, Integer durationMs) {
            return new SendResult(false, errorCode, errorMessage, durationMs);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public Integer getDurationMs() {
            return durationMs;
        }
    }
}
