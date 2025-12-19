package com.bluecone.app.billing.notification;

import com.bluecone.app.billing.domain.enums.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 站内通知发送器
 * 当前实现为简化版本，仅记录日志
 * 实际应该调用站内通知服务（如消息中心）
 */
@Slf4j
@Component
public class InAppNotificationSender implements NotificationSender {
    
    @Override
    public SendResult send(
        NotificationChannel channel,
        Long tenantId,
        String reminderType,
        String planName,
        String expireAt,
        int daysRemaining
    ) {
        if (channel != NotificationChannel.IN_APP) {
            return SendResult.failure("不支持的通知渠道: " + channel);
        }
        
        try {
            // 构建通知内容
            String title = buildTitle(reminderType, daysRemaining);
            String content = buildContent(planName, expireAt, daysRemaining);
            
            // TODO: 实际应该调用站内通知服务
            // notificationService.send(tenantId, title, content);
            
            log.info("[in-app-notification] 站内通知发送成功，tenantId={}, title={}, content={}", 
                    tenantId, title, content);
            
            return SendResult.success("站内通知发送成功", String.format("{\"title\":\"%s\",\"content\":\"%s\"}", title, content));
            
        } catch (Exception e) {
            log.error("[in-app-notification] 站内通知发送失败，tenantId={}", tenantId, e);
            return SendResult.failure("站内通知发送失败: " + e.getMessage());
        }
    }
    
    private String buildTitle(String reminderType, int daysRemaining) {
        if (daysRemaining > 0) {
            return String.format("订阅即将到期提醒（剩余%d天）", daysRemaining);
        } else if (daysRemaining == 0) {
            return "订阅今日到期提醒";
        } else {
            return "订阅已到期，进入宽限期";
        }
    }
    
    private String buildContent(String planName, String expireAt, int daysRemaining) {
        if (daysRemaining > 0) {
            return String.format("您的 %s 套餐将于 %s 到期，剩余 %d 天。为避免服务中断，请及时续费。", 
                    planName, expireAt, daysRemaining);
        } else if (daysRemaining == 0) {
            return String.format("您的 %s 套餐将于今日（%s）到期。为避免服务中断，请立即续费。", 
                    planName, expireAt);
        } else {
            return String.format("您的 %s 套餐已于 %s 到期，当前处于宽限期。宽限期内部分功能受限，请尽快续费以恢复全部功能。", 
                    planName, expireAt);
        }
    }
}
