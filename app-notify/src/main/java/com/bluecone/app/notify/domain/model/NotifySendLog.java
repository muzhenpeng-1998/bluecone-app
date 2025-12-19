package com.bluecone.app.notify.domain.model;

import com.bluecone.app.notify.api.enums.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知发送日志领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifySendLog {
    
    private Long id;
    private Long taskId;
    private Long tenantId;
    private Long userId;
    private NotificationChannel channel;
    private String bizType;
    private String bizId;
    private String title;
    private String content;
    private String recipient;
    private String sendStatus;
    private String errorCode;
    private String errorMessage;
    private String providerResponseJson;
    private Integer sendDurationMs;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    
    /**
     * 创建成功日志
     */
    public static NotifySendLog success(NotifyTask task, String recipient, Integer durationMs) {
        return NotifySendLog.builder()
                .taskId(task.getId())
                .tenantId(task.getTenantId())
                .userId(task.getUserId())
                .channel(task.getChannel())
                .bizType(task.getBizType())
                .bizId(task.getBizId())
                .title(task.getTitle())
                .content(task.getContent())
                .recipient(recipient)
                .sendStatus("SUCCESS")
                .sendDurationMs(durationMs)
                .sentAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建失败日志
     */
    public static NotifySendLog failure(NotifyTask task, String recipient, String errorCode, 
                                       String errorMessage, Integer durationMs) {
        return NotifySendLog.builder()
                .taskId(task.getId())
                .tenantId(task.getTenantId())
                .userId(task.getUserId())
                .channel(task.getChannel())
                .bizType(task.getBizType())
                .bizId(task.getBizId())
                .title(task.getTitle())
                .content(task.getContent())
                .recipient(recipient)
                .sendStatus("FAILED")
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .sendDurationMs(durationMs)
                .sentAt(LocalDateTime.now())
                .build();
    }
}
