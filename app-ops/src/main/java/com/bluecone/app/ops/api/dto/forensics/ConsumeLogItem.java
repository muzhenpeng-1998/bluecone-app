package com.bluecone.app.ops.api.dto.forensics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消费日志记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumeLogItem {
    
    /**
     * 日志ID
     */
    private Long id;
    
    /**
     * 消费者名称
     */
    private String consumerName;
    
    /**
     * 事件唯一ID
     */
    private String eventId;
    
    /**
     * 事件类型
     */
    private String eventType;
    
    /**
     * 消费状态：SUCCESS/FAILED/PROCESSING
     */
    private String status;
    
    /**
     * 幂等键
     */
    private String idempotencyKey;
    
    /**
     * 消费结果（JSON）
     */
    private String consumeResult;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 消费时间
     */
    private LocalDateTime consumedAt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
