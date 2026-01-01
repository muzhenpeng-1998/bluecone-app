package com.bluecone.app.infra.integration.idempotency;

import java.time.Duration;

/**
 * 集成幂等服务接口。
 * <p>
 * 用于外部集成（如微信回调）的幂等检查，基于 Redis SETNX 实现。
 * </p>
 */
public interface IntegrationIdempotencyService {

    /**
     * 尝试获取幂等锁（SETNX + EXPIRE）。
     * <p>
     * 如果 key 不存在，则设置 key 并返回 true（首次调用）。
     * 如果 key 已存在，则返回 false（重复调用）。
     * </p>
     *
     * @param key 幂等 key
     * @param ttl 过期时间
     * @return true 表示首次调用（未重复），false 表示重复调用
     */
    boolean tryAcquire(String key, Duration ttl);

    /**
     * 检查 key 是否已存在（是否已处理过）。
     *
     * @param key 幂等 key
     * @return true 表示已存在（已处理过），false 表示不存在（未处理）
     */
    boolean exists(String key);

    /**
     * 删除幂等 key（用于测试或手动清理）。
     *
     * @param key 幂等 key
     */
    void delete(String key);
}

