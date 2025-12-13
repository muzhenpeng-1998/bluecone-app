package com.bluecone.app.core.create.api;

import java.time.Duration;

/**
 * 幂等创建请求参数。
 *
 * @param tenantId          租户 ID（必须 > 0）
 * @param bizType           幂等业务类型，例如 ORDER_CREATE / STORE_CREATE
 * @param resourceType      资源类型前缀，例如 ord/sto/ten/usr/pay，用于 public_id
 * @param idemKey           幂等键（来自 Idempotency-Key 或业务生成）
 * @param requestHash       请求内容摘要（SHA-256 64 HEX）
 * @param ttl               幂等记录有效期（默认 24h）
 * @param lockTtl           单次执行租约时长（默认 30s）
 * @param txMode            事务模式，默认 REQUIRES_NEW
 * @param waitForCompletion 是否在存在并发执行时等待结果
 * @param waitMax           最大等待时间（仅在 waitForCompletion=true 时生效）
 */
public record CreateRequest(
        long tenantId,
        String bizType,
        String resourceType,
        String idemKey,
        String requestHash,
        Duration ttl,
        Duration lockTtl,
        TxMode txMode,
        boolean waitForCompletion,
        Duration waitMax
) {

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    private static final Duration DEFAULT_LOCK_TTL = Duration.ofSeconds(30);

    public CreateRequest {
        if (tenantId <= 0) {
            throw new IllegalArgumentException("tenantId 必须大于 0");
        }
        if (bizType == null || bizType.isBlank()) {
            throw new IllegalArgumentException("bizType 不能为空");
        }
        if (resourceType == null || resourceType.isBlank()) {
            throw new IllegalArgumentException("resourceType 不能为空");
        }
        if (!resourceType.matches("[a-z0-9]{2,10}")) {
            throw new IllegalArgumentException("resourceType 必须满足正则 [a-z0-9]{2,10}");
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
        if (txMode == null) {
            txMode = TxMode.REQUIRES_NEW;
        }
        if (waitForCompletion) {
            if (waitMax != null && (waitMax.isNegative() || waitMax.isZero())) {
                throw new IllegalArgumentException("waitMax 必须为正数");
            }
        }
    }
}

