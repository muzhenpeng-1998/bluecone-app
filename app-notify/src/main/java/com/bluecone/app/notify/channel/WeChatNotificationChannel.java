package com.bluecone.app.notify.channel;

import com.bluecone.app.notify.domain.model.NotifyTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 微信订阅消息通知渠道（预留）
 * 通过微信小程序订阅消息 API 发送通知
 */
@Slf4j
@Component
public class WeChatNotificationChannel implements NotificationChannel {
    
    @Override
    public com.bluecone.app.notify.api.enums.NotificationChannel getChannelType() {
        return com.bluecone.app.notify.api.enums.NotificationChannel.WECHAT;
    }
    
    @Override
    public SendResult send(NotifyTask task, String recipient) {
        long startTime = System.currentTimeMillis();
        
        // TODO: 实现微信订阅消息发送
        // 1. 获取 access_token
        // 2. 调用微信订阅消息 API
        // 3. 处理返回结果
        
        log.warn("WeChat notification channel not implemented yet, recipient={}", recipient);
        
        int durationMs = (int) (System.currentTimeMillis() - startTime);
        return SendResult.failure("NOT_IMPLEMENTED", "WeChat channel not implemented", durationMs);
    }
}
