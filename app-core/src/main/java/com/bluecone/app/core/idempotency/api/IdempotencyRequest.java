package com.bluecone.app.core.idempotency.api;

import java.time.Duration;

/**
 * 幂等请求参数，包含租户、业务类型、幂等键及请求摘要等信息。
 *
 * @param tenantId         租户 ID，必须大于 0
 * @param bizType          业务类型，例如 ORDER_CREATE / STORE_CREATE
 * @param idemKey          幂等键（通常来自 HTTP Idempotency-Key 或业务生成）
 * @param requestHash      请求内容摘要（SHA-256 hex，长度 64）
 * @param ttl              幂等记录有效期（默认 24 小时）
 * @param lockTtl          单次执行租约时长（默认 30 秒）
 * @param waitForCompletion 是否等待并发请求完成后重放结果
 * @param waitMax          最长等待时间（仅在 waitForCompletion=true 时生效）
 */
public record IdempotencyRequest(
        long tenantId,
        String bizType,
        String idemKey,
        String requestHash,
        Duration ttl,
        Duration lockTtl,
        boolean waitForCompletion,
        Duration waitMax
) {

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    private static final Duration DEFAULT_LOCK_TTL = Duration.ofSeconds(30);

    public IdempotencyRequest {
        if (tenantId <= 0) {
            throw new IllegalArgumentException("tenantId 必须大于 0");
        }
        if (bizType == null || bizType.isBlank()) {
            throw new IllegalArgumentException("bizType 不能为空");
        }
        if (idemKey == null || idemKey.isBlank()) {
            throw new IllegalArgumentException("idemKey 不能为空");
        }
        if (requestHash == null || requestHash.length() != 64) {
            throw new IllegalArgumentException("requestHash 必须为 64 位十六进制字符串（SHA-256 摘要）");
        }
        Duration effectiveTtl = (ttl == null ? DEFAULT_TTL : ttl);
        if (effectiveTtl.isNegative() || effectiveTtl.isZero()) {
            throw new IllegalArgumentException("ttl 必须为正数");
        }
        Duration effectiveLockTtl = (lockTtl == null ? DEFAULT_LOCK_TTL : lockTtl);
        if (effectiveLockTtl.isNegative() || effectiveLockTtl.isZero()) {
            throw new IllegalArgumentException("lockTtl 必须为正数");
        }
        ttl = effectiveTtl;
        lockTtl = effectiveLockTtl;
        if (waitForCompletion) {
            if (waitMax != null && (waitMax.isNegative() || waitMax.isZero())) {
                throw new IllegalArgumentException("waitMax 必须为正数");
            }
        }
    }
}

