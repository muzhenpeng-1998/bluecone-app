package com.bluecone.app.notify.channel;

import com.bluecone.app.notify.domain.model.NotifyTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 邮件通知渠道
 * 通过 JavaMailSender 发送邮件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationChannel implements NotificationChannel {
    
    private final JavaMailSender mailSender;
    
    @Override
    public com.bluecone.app.notify.api.enums.NotificationChannel getChannelType() {
        return com.bluecone.app.notify.api.enums.NotificationChannel.EMAIL;
    }
    
    @Override
    public SendResult send(NotifyTask task, String recipient) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 构建邮件
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipient);
            message.setSubject(task.getTitle() != null ? task.getTitle() : "通知");
            message.setText(task.getContent());
            message.setFrom("noreply@bluecone.com"); // TODO: 从配置读取
            
            log.info("Sending email to {}, subject={}", recipient, task.getTitle());
            
            // 发送邮件
            mailSender.send(message);
            
            int durationMs = (int) (System.currentTimeMillis() - startTime);
            log.info("Email sent successfully to {} in {}ms", recipient, durationMs);
            
            return SendResult.success(durationMs);
            
        } catch (MailException e) {
            int durationMs = (int) (System.currentTimeMillis() - startTime);
            log.error("Failed to send email to {}", recipient, e);
            return SendResult.failure("EMAIL_ERROR", e.getMessage(), durationMs);
        }
    }
}
