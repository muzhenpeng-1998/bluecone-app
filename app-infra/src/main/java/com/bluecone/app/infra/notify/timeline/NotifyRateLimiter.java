package com.bluecone.app.infra.notify.timeline;

import com.bluecone.app.infra.notify.model.NotificationTask;
import com.bluecone.app.infra.redis.core.RedisKeyBuilder;
import com.bluecone.app.infra.redis.core.RedisKeyNamespace;
import com.bluecone.app.infra.redis.core.RedisOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;

/**
 * 基于 Redis 计数的限流器（Timeline 层）。
 */
public class NotifyRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(NotifyRateLimiter.class);

    private final RedisOps redisOps;
    private final RedisKeyBuilder redisKeyBuilder;

    public NotifyRateLimiter(RedisOps redisOps, RedisKeyBuilder redisKeyBuilder) {
        this.redisOps = Objects.requireNonNull(redisOps, "redisOps must not be null");
        this.redisKeyBuilder = Objects.requireNonNull(redisKeyBuilder, "redisKeyBuilder must not be null");
    }

    /**
     * 在租户+场景+通道维度尝试消费配额。
     *
     * @param task          通道任务
     * @param maxPerMinute  每分钟上限
     * @return true 表示未超限
     */
    public boolean tryConsume(NotificationTask task, int maxPerMinute) {
        String key = redisKeyBuilder.build(RedisKeyNamespace.NOTIFY, task.getScenarioCode(),
                "rate", task.getChannel().getCode());
        Long count = redisOps.incr(key, 1);
        if (count != null && count == 1) {
            redisOps.expire(key, Duration.ofMinutes(1));
        }
        boolean allowed = count == null || count <= maxPerMinute;
        if (!allowed) {
            log.warn("[NotifyRateLimiter] exceed limit key={} count={} max={}", key, count, maxPerMinute);
        }
        return allowed;
    }
}
