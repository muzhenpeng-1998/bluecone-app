package com.bluecone.app.infra.redis.lock;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.bluecone.app.infra.redis.core.RedisKeyBuilder;
import com.bluecone.app.infra.redis.core.RedisKeyNamespace;
import com.bluecone.app.infra.redis.core.RedisOps;

/**
 * 基于 Redis 的分布式锁实现，使用 SETNX + 过期时间实现互斥。
 * <p>设计要点：所有 Redis 访问均通过 RedisOps，Key 统一使用 RedisKeyBuilder 规范化。</p>
 */
@Component
public class RedisDistributedLock implements DistributedLock {

    private static final Logger log = LoggerFactory.getLogger(RedisDistributedLock.class);
    private static final long RETRY_INTERVAL_MS = 50L;

    private final RedisOps redisOps;
    private final RedisKeyBuilder keyBuilder;

    public RedisDistributedLock(RedisOps redisOps, RedisKeyBuilder keyBuilder) {
        this.redisOps = redisOps;
        this.keyBuilder = keyBuilder;
    }

    @Override
    public boolean tryLock(String bizKey, String ownerId, long waitTimeMs, long leaseTimeMs) {
        Assert.hasText(bizKey, "bizKey must not be blank");
        Assert.hasText(ownerId, "ownerId must not be blank");
        if (leaseTimeMs <= 0) {
            throw new IllegalArgumentException("leaseTimeMs must be positive");
        }
        String redisKey = keyBuilder.build(RedisKeyNamespace.LOCK, bizKey);
        long deadline = waitTimeMs > 0 ? System.currentTimeMillis() + waitTimeMs : 0L;
        Duration leaseDuration = Duration.ofMillis(leaseTimeMs);
        do {
            Boolean success = redisOps.setIfAbsent(redisKey, ownerId, leaseDuration);
            if (Boolean.TRUE.equals(success)) {
                return true;
            }
            if (waitTimeMs <= 0) {
                break;
            }
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                break;
            }
            sleepQuietly(Math.min(RETRY_INTERVAL_MS, remaining));
        } while (true);
        return false;
    }

    @Override
    public void unlock(String bizKey, String ownerId) {
        Assert.hasText(bizKey, "bizKey must not be blank");
        Assert.hasText(ownerId, "ownerId must not be blank");
        String redisKey = keyBuilder.build(RedisKeyNamespace.LOCK, bizKey);
        String value = redisOps.getString(redisKey);
        if (ownerId.equals(value)) {
            redisOps.delete(redisKey);
        } else if (value != null) {
            log.debug("skip unlock because ownerId mismatch, key={}, ownerId={}, current={}", redisKey, ownerId, value);
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
