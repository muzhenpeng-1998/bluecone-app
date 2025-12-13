package com.bluecone.app.core.idempotency.spi;

import java.time.Instant;
import java.util.Optional;

import com.bluecone.app.core.idempotency.domain.IdempotencyRecord;

/**
 * 幂等记录存储接口，由基础设施层（如 MyBatis + MySQL）实现。
 */
public interface IdempotencyRepository {

    /**
     * 根据幂等键查询记录。
     *
     * @param tenantId 租户 ID
     * @param bizType  业务类型
     * @param idemKey  幂等键
     * @return 可能存在的幂等记录
     */
    Optional<IdempotencyRecord> find(long tenantId, String bizType, String idemKey);

    /**
     * 尝试获得执行权（插入新记录或夺回过期租约）。
     *
     * @param command 尝试获取命令
     * @return 获取结果
     */
    AcquireResult tryAcquire(AcquireCommand command);

    /**
     * 标记成功并更新结果。
     *
     * @param command 成功命令
     */
    void markSuccess(MarkSuccessCommand command);

    /**
     * 标记失败并记录错误。
     *
     * @param command 失败命令
     */
    void markFailed(MarkFailedCommand command);

    /**
     * 尝试获取执行权的命令。
     */
    record AcquireCommand(
            long tenantId,
            String bizType,
            String idemKey,
            String requestHash,
            Instant expireAt,
            Instant lockUntil
    ) {
    }

    /**
     * 获取执行权的结果。
     */
    record AcquireResult(
            AcquireState state,
            IdempotencyRecord record
    ) {
    }

    /**
     * 获取执行权结果状态。
     */
    enum AcquireState {
        /**
         * 获得执行权，需要执行真实业务。
         */
        ACQUIRED,
        /**
         * 已存在成功记录且未过期，可以重放结果。
         */
        REPLAY_SUCCEEDED,
        /**
         * 当前有正在处理的请求。
         */
        IN_PROGRESS,
        /**
         * 幂等键对应请求摘要不一致，发生冲突。
         */
        CONFLICT,
        /**
         * 可重试状态，通常用于失败后允许重新执行的场景。
         */
        RETRYABLE
    }

    /**
     * 标记成功命令。
     */
    record MarkSuccessCommand(
            long tenantId,
            String bizType,
            String idemKey,
            String requestHash,
            String resultRef,
            String resultJson,
            Instant expireAt
    ) {
    }

    /**
     * 标记失败命令。
     */
    record MarkFailedCommand(
            long tenantId,
            String bizType,
            String idemKey,
            String requestHash,
            String errorCode,
            String errorMsg,
            Instant expireAt
    ) {
    }
}

