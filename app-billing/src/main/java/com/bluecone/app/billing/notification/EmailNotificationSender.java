package com.bluecone.app.billing.notification;

import com.bluecone.app.billing.domain.enums.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 邮件通知发送器
 * 当前实现为简化版本，仅记录日志
 * 实际应该调用邮件服务（如 SMTP 服务）
 */
@Slf4j
@Component
public class EmailNotificationSender implements NotificationSender {
    
    @Override
    public SendResult send(
        NotificationChannel channel,
        Long tenantId,
        String reminderType,
        String planName,
        String expireAt,
        int daysRemaining
    ) {
        if (channel != NotificationChannel.EMAIL) {
            return SendResult.failure("不支持的通知渠道: " + channel);
        }
        
        try {
            // 构建邮件内容
            String subject = buildSubject(reminderType, daysRemaining);
            String body = buildBody(planName, expireAt, daysRemaining);
            
            // TODO: 实际应该调用邮件服务
            // String recipientEmail = getTenantEmail(tenantId);
            // emailService.send(recipientEmail, subject, body);
            
            log.info("[email-notification] 邮件通知发送成功，tenantId={}, subject={}", tenantId, subject);
            
            return SendResult.success("邮件通知发送成功", String.format("{\"subject\":\"%s\"}", subject));
            
        } catch (Exception e) {
            log.error("[email-notification] 邮件通知发送失败，tenantId={}", tenantId, e);
            return SendResult.failure("邮件通知发送失败: " + e.getMessage());
        }
    }
    
    private String buildSubject(String reminderType, int daysRemaining) {
        if (daysRemaining > 0) {
            return String.format("[BlueCone] 订阅即将到期提醒（剩余%d天）", daysRemaining);
        } else if (daysRemaining == 0) {
            return "[BlueCone] 订阅今日到期提醒";
        } else {
            return "[BlueCone] 订阅已到期，进入宽限期";
        }
    }
    
    private String buildBody(String planName, String expireAt, int daysRemaining) {
        StringBuilder body = new StringBuilder();
        body.append("尊敬的用户，\n\n");
        
        if (daysRemaining > 0) {
            body.append(String.format("您的 %s 套餐将于 %s 到期，剩余 %d 天。\n\n", planName, expireAt, daysRemaining));
            body.append("为避免服务中断，请及时续费。\n\n");
        } else if (daysRemaining == 0) {
            body.append(String.format("您的 %s 套餐将于今日（%s）到期。\n\n", planName, expireAt));
            body.append("为避免服务中断，请立即续费。\n\n");
        } else {
            body.append(String.format("您的 %s 套餐已于 %s 到期，当前处于宽限期。\n\n", planName, expireAt));
            body.append("宽限期内部分功能受限，请尽快续费以恢复全部功能。\n\n");
        }
        
        body.append("点击续费：[续费链接]\n\n");
        body.append("如有疑问，请联系客服。\n\n");
        body.append("BlueCone 团队");
        
        return body.toString();
    }
}
