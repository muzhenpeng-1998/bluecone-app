package com.bluecone.app.core.event.consume.spi;

import com.bluecone.app.id.core.Ulid128;

import java.time.Instant;
import java.util.Optional;

/**
 * 事件消费去重存储 SPI。
 *
 * <p>负责基于数据库实现 consumer_group + event_id 维度的消费幂等。</p>
 */
public interface EventDedupRepository {

    AcquireConsumeResult tryAcquire(AcquireConsumeCommand command);

    Optional<ConsumeRecord> find(String consumerGroup, Ulid128 eventId);

    void markSuccess(MarkConsumeSuccessCommand command);

    void markFailed(MarkConsumeFailedCommand command);

    /**
     * 获取消费权命令。
     */
    record AcquireConsumeCommand(
            long tenantId,
            String consumerGroup,
            Ulid128 eventId,
            String eventType,
            Instant now,
            Instant lockedUntil
    ) {
    }

    /**
     * 获取消费权结果。
     */
    record AcquireConsumeResult(
            AcquireConsumeState state,
            ConsumeRecord record
    ) {
    }

    /**
     * 获取消费权状态。
     */
    enum AcquireConsumeState {
        ACQUIRED,
        REPLAY_SUCCEEDED,
        IN_PROGRESS,
        RETRYABLE_FAILED,
        CONFLICT
    }

    /**
     * 消费记录快照。
     */
    record ConsumeRecord(
            long tenantId,
            String consumerGroup,
            Ulid128 eventId,
            String eventType,
            int status,
            Instant lockedUntil,
            Instant nextRetryAt,
            int retryCount,
            String errorMsg
    ) {
    }

    /**
     * 标记消费成功命令。
     */
    record MarkConsumeSuccessCommand(
            String consumerGroup,
            Ulid128 eventId,
            Instant processedAt
    ) {
    }

    /**
     * 标记消费失败命令。
     */
    record MarkConsumeFailedCommand(
            String consumerGroup,
            Ulid128 eventId,
            String errorMsg,
            Instant nextRetryAt,
            int retryCount
    ) {
    }
}

