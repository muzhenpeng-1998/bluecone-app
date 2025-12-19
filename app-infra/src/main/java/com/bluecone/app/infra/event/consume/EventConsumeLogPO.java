package com.bluecone.app.infra.event.consume;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事件消费日志持久化对象
 * 对应表：bc_event_consume_log
 * 用于消费者幂等性保证
 */
@Data
@TableName("bc_event_consume_log")
public class EventConsumeLogPO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long tenantId;
    
    /**
     * 消费者名称：CouponConsumer/WalletConsumer/PointsConsumer
     */
    private String consumerName;
    
    /**
     * 事件唯一ID（对应 bc_outbox_event.event_id）
     */
    private String eventId;
    
    /**
     * 事件类型（冗余，便于查询）
     */
    private String eventType;
    
    /**
     * 消费状态：SUCCESS/FAILED/PROCESSING
     */
    private String status;
    
    /**
     * 幂等键（业务侧幂等键，如 orderId:checkout）
     */
    private String idempotencyKey;
    
    /**
     * 消费结果（JSON）
     */
    private String consumeResult;
    
    /**
     * 错误信息（消费失败时记录）
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
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
