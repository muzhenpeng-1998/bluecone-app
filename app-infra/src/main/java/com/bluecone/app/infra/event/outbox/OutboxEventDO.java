package com.bluecone.app.infra.event.outbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Outbox 事件持久化实体，对应表 bc_outbox_message（见 outbox/README.md）。
 * 当前阶段仅用于将 DomainEvent 序列化后落表，不做消费/投递。
 */
@Data
public class OutboxEventDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /**
     * 聚合根类型：ORDER / PAYMENT / STORE 等。
     */
    private String aggregateType;

    /**
     * 聚合根 ID，例如订单 ID、支付单 ID。
     */
    private String aggregateId;

    /**
     * 事件类型语义名，如 order.created / payment.success。
     */
    private String eventType;

    /**
     * 事件业务内容，序列化后的 JSON。
     */
    private String eventBody;

    /**
     * 事件状态：0-NEW，1-DONE，2-FAILED，9-IGNORED。
     */
    private Integer status;

    /**
     * 已重试次数。
     */
    private Integer retryCount;

    /**
     * 可被拉取的时间（用于重试/延迟调度）。
     */
    private LocalDateTime availableAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}


