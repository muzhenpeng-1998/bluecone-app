package com.bluecone.app.notify.channel;

import com.bluecone.app.notify.domain.model.NotifyTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 站内信通知渠道
 * 将通知写入站内信箱表（或通过消息队列发送到前端）
 */
@Slf4j
@Component
public class InAppNotificationChannel implements NotificationChannel {
    
    @Override
    public com.bluecone.app.notify.api.enums.NotificationChannel getChannelType() {
        return com.bluecone.app.notify.api.enums.NotificationChannel.IN_APP;
    }
    
    @Override
    public SendResult send(NotifyTask task, String recipient) {
        long startTime = System.currentTimeMillis();
        
        try {
            // TODO: 实现站内信发送逻辑
            // 1. 写入 bc_user_inbox 表
            // 2. 或推送到 WebSocket/SSE 连接
            // 3. 或发布到消息队列由前端订阅
            
            log.info("Sending in-app notification to user={}, title={}", task.getUserId(), task.getTitle());
            
            // 模拟发送
            Thread.sleep(50);
            
            int durationMs = (int) (System.currentTimeMillis() - startTime);
            log.info("In-app notification sent successfully in {}ms", durationMs);
            
            return SendResult.success(durationMs);
            
        } catch (Exception e) {
            int durationMs = (int) (System.currentTimeMillis() - startTime);
            log.error("Failed to send in-app notification", e);
            return SendResult.failure("IN_APP_ERROR", e.getMessage(), durationMs);
        }
    }
}
