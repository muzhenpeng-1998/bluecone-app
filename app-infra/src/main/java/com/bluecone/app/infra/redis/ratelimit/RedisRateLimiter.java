package com.bluecone.app.infra.redis.ratelimit;

import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.bluecone.app.infra.redis.core.RedisKeyBuilder;
import com.bluecone.app.infra.redis.core.RedisKeyNamespace;
import com.bluecone.app.infra.redis.core.RedisOps;

/**
 * 基于 Redis 的固定窗口限流器，使用 INCR + EXPIRE 实现。
 */
@Component
public class RedisRateLimiter {

    private final RedisOps redisOps;
    private final RedisKeyBuilder keyBuilder;

    public RedisRateLimiter(RedisOps redisOps, RedisKeyBuilder keyBuilder) {
        this.redisOps = redisOps;
        this.keyBuilder = keyBuilder;
    }

    /**
     * 尝试在限流窗口内获取一次调用配额。
     *
     * @param bizKey         业务键（未含环境、租户前缀）
     * @param limit          最大次数
     * @param windowSeconds  固定窗口秒数
     * @return true 表示允许，false 表示被限流
     */
    public boolean tryAcquire(String bizKey, int limit, int windowSeconds) {
        Assert.hasText(bizKey, "bizKey must not be blank");
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds must be positive");
        }
        long bucketStart = currentBucketStart(windowSeconds);
        String redisKey = keyBuilder.build(RedisKeyNamespace.RATE_LIMIT, bizKey, String.valueOf(bucketStart));
        Long count = redisOps.incr(redisKey, 1);
        if (count != null && count == 1) {
            redisOps.expire(redisKey, Duration.ofSeconds(windowSeconds));
        }
        return count != null && count <= limit;
    }

    private long currentBucketStart(int windowSeconds) {
        long currentSeconds = System.currentTimeMillis() / 1000;
        return currentSeconds / windowSeconds * windowSeconds;
    }
}
