package com.bluecone.app.notify.channel;

import com.bluecone.app.notify.domain.model.NotifyTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 短信通知渠道（预留）
 * 通过短信服务商 API 发送短信
 */
@Slf4j
@Component
public class SmsNotificationChannel implements NotificationChannel {
    
    @Override
    public com.bluecone.app.notify.api.enums.NotificationChannel getChannelType() {
        return com.bluecone.app.notify.api.enums.NotificationChannel.SMS;
    }
    
    @Override
    public SendResult send(NotifyTask task, String recipient) {
        long startTime = System.currentTimeMillis();
        
        // TODO: 实现短信发送
        // 1. 集成短信服务商（阿里云/腾讯云）
        // 2. 调用短信发送 API
        // 3. 处理返回结果
        
        log.warn("SMS notification channel not implemented yet, recipient={}", recipient);
        
        int durationMs = (int) (System.currentTimeMillis() - startTime);
        return SendResult.failure("NOT_IMPLEMENTED", "SMS channel not implemented", durationMs);
    }
}
