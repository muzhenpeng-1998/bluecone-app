package com.bluecone.app.infra.redis.idempotent;

import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.bluecone.app.infra.redis.core.RedisKeyBuilder;
import com.bluecone.app.infra.redis.core.RedisKeyNamespace;
import com.bluecone.app.infra.redis.core.RedisOps;

/**
 * 基于 Redis 的幂等执行器，采用 SETNX 占位保证同一业务键只执行一次。
 */
@Component
public class RedisIdempotentExecutor {

    private static final String STATE_PENDING = "PENDING";
    private static final String STATE_SUCCESS = "SUCCESS";

    private final RedisOps redisOps;
    private final RedisKeyBuilder keyBuilder;

    public RedisIdempotentExecutor(RedisOps redisOps, RedisKeyBuilder keyBuilder) {
        this.redisOps = redisOps;
        this.keyBuilder = keyBuilder;
    }

    /**
     * 尝试进入幂等执行区。
     *
     * @param bizKey        业务键
     * @param scene         幂等场景
     * @param expireSeconds 过期秒数
     * @return true 表示允许执行业务逻辑；false 表示重复请求
     */
    public boolean tryEnter(String bizKey, IdempotentScene scene, int expireSeconds) {
        Assert.hasText(bizKey, "bizKey must not be blank");
        if (expireSeconds <= 0) {
            throw new IllegalArgumentException("expireSeconds must be positive");
        }
        String redisKey = keyBuilder.build(RedisKeyNamespace.IDEMPOTENT, bizKey, scene.name().toLowerCase());
        Duration ttl = Duration.ofSeconds(expireSeconds);
        Boolean success = redisOps.setIfAbsent(redisKey, STATE_PENDING, ttl);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 在业务执行成功后更新状态，便于排查。
     *
     * @param bizKey        业务键
     * @param scene         幂等场景
     * @param expireSeconds 过期秒数
     */
    public void onSuccess(String bizKey, IdempotentScene scene, int expireSeconds) {
        String redisKey = keyBuilder.build(RedisKeyNamespace.IDEMPOTENT, bizKey, scene.name().toLowerCase());
        redisOps.setString(redisKey, STATE_SUCCESS, Duration.ofSeconds(expireSeconds));
    }
}
