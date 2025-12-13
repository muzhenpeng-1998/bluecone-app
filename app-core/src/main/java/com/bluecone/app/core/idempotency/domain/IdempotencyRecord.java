package com.bluecone.app.core.idempotency.domain;

import java.time.Instant;

/**
 * 幂等记录领域对象，对应幂等表的一行数据。
 *
 * @param id          自增主键
 * @param tenantId    租户 ID
 * @param bizType     业务类型
 * @param idemKey     幂等键
 * @param requestHash 请求摘要（SHA-256 hex）
 * @param status      状态
 * @param resultRef   结果引用（如 public_id）
 * @param resultJson  结果 JSON（可选，小体积）
 * @param errorCode   错误码
 * @param errorMsg    错误信息
 * @param expireAt    记录失效时间
 * @param lockUntil   执行租约到期时间
 * @param version     版本号（乐观锁）
 * @param createdAt   创建时间
 * @param updatedAt   更新时间
 */
public record IdempotencyRecord(
        Long id,
        long tenantId,
        String bizType,
        String idemKey,
        String requestHash,
        IdemStatus status,
        String resultRef,
        String resultJson,
        String errorCode,
        String errorMsg,
        Instant expireAt,
        Instant lockUntil,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
}

