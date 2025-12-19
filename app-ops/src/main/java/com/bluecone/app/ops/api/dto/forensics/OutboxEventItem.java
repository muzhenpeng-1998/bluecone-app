package com.bluecone.app.ops.api.dto.forensics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Outbox 事件记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEventItem {
    
    /**
     * 事件ID（数据库主键）
     */
    private Long id;
    
    /**
     * 聚合根类型
     */
    private String aggregateType;
    
    /**
     * 聚合根ID
     */
    private String aggregateId;
    
    /**
     * 事件类型
     */
    private String eventType;
    
    /**
     * 事件唯一ID（UUID）
     */
    private String eventId;
    
    /**
     * 状态：NEW/SENT/FAILED/DEAD
     */
    private String status;
    
    /**
     * 已重试次数
     */
    private Integer retryCount;
    
    /**
     * 最大重试次数
     */
    private Integer maxRetryCount;
    
    /**
     * 下次重试时间
     */
    private LocalDateTime nextRetryAt;
    
    /**
     * 最后一次错误信息
     */
    private String lastError;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 投递成功时间
     */
    private LocalDateTime sentAt;
    
    /**
     * 事件载荷（JSON，可选暴露）
     */
    private String eventPayload;
    
    /**
     * 事件元数据（JSON，可选暴露）
     */
    private String eventMetadata;
}
