package com.bluecone.app.notify.domain.model;

import com.bluecone.app.notify.api.enums.NotificationChannel;
import com.bluecone.app.notify.api.enums.NotificationTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知任务领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyTask {
    
    private Long id;
    private Long tenantId;
    private Long userId;
    private String bizType;
    private String bizId;
    private String templateCode;
    private NotificationChannel channel;
    private Integer priority;
    private String variablesJson;
    private String title;
    private String content;
    private String idempotencyKey;
    private NotificationTaskStatus status;
    private Integer retryCount;
    private Integer maxRetryCount;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private LocalDateTime rateLimitCheckedAt;
    private Boolean rateLimitPassed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime sentAt;
    
    /**
     * 是否可以发送
     */
    public boolean canSend() {
        return status == NotificationTaskStatus.PENDING || 
               (status == NotificationTaskStatus.FAILED && canRetry());
    }
    
    /**
     * 是否可以重试
     */
    public boolean canRetry() {
        return retryCount < maxRetryCount && 
               (nextRetryAt == null || LocalDateTime.now().isAfter(nextRetryAt));
    }
    
    /**
     * 标记为发送中
     */
    public void markAsSending() {
        this.status = NotificationTaskStatus.SENDING;
    }
    
    /**
     * 标记为已发送
     */
    public void markAsSent() {
        this.status = NotificationTaskStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }
    
    /**
     * 标记为失败
     */
    public void markAsFailed(String errorMessage) {
        this.status = NotificationTaskStatus.FAILED;
        this.lastError = errorMessage;
        this.retryCount++;
        // 指数退避：2^retryCount 分钟
        int backoffMinutes = (int) Math.pow(2, retryCount);
        this.nextRetryAt = LocalDateTime.now().plusMinutes(backoffMinutes);
    }
    
    /**
     * 标记为频控限制
     */
    public void markAsRateLimited() {
        this.status = NotificationTaskStatus.RATE_LIMITED;
        this.rateLimitCheckedAt = LocalDateTime.now();
        this.rateLimitPassed = false;
    }
    
    /**
     * 标记为已取消
     */
    public void markAsCancelled() {
        this.status = NotificationTaskStatus.CANCELLED;
    }
    
    /**
     * 通过频控检查
     */
    public void passRateLimitCheck() {
        this.rateLimitCheckedAt = LocalDateTime.now();
        this.rateLimitPassed = true;
    }
}
