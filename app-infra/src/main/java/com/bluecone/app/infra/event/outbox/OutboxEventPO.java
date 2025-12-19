package com.bluecone.app.infra.event.outbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Outbox 事件持久化对象
 * 对应表：bc_outbox_event
 */
@Data
@TableName(value = "bc_outbox_event", autoResultMap = true)
public class OutboxEventPO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long tenantId;
    
    private Long storeId;
    
    /**
     * 聚合根类型：ORDER/PAYMENT/REFUND 等
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
     * 事件唯一ID
     */
    private String eventId;
    
    /**
     * 事件载荷（JSON）
     */
    private String eventPayload;
    
    /**
     * 事件元数据（JSON）
     */
    private String eventMetadata;
    
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
}
