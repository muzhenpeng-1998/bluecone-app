// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/core/RedisEventConsumptionTracker.java
package com.bluecone.app.infra.outbox.core;

import com.bluecone.app.infra.outbox.config.OutboxProperties;
import com.bluecone.app.infra.redis.core.RedisKeyBuilder;
import com.bluecone.app.infra.redis.core.RedisKeyNamespace;
import com.bluecone.app.infra.redis.core.RedisOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

/**
 * 基于 Redis 的消费去重，实现幂等消费。
 *
 * <p>通过 SETNX + TTL 标记 handler 已处理的 eventId，重复到达则直接跳过。</p>
 */
@Component
public class RedisEventConsumptionTracker implements EventConsumptionTracker {

    private static final Logger log = LoggerFactory.getLogger(RedisEventConsumptionTracker.class);

    private final RedisOps redisOps;
    private final RedisKeyBuilder redisKeyBuilder;
    private final OutboxProperties properties;

    public RedisEventConsumptionTracker(final RedisOps redisOps,
                                        final RedisKeyBuilder redisKeyBuilder,
                                        final OutboxProperties properties) {
        this.redisOps = Objects.requireNonNull(redisOps, "redisOps must not be null");
        this.redisKeyBuilder = Objects.requireNonNull(redisKeyBuilder, "redisKeyBuilder must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public boolean tryMarkProcessing(final String consumerName, final String eventId) {
        String key = redisKeyBuilder.buildForGlobal(RedisKeyNamespace.EVENT, consumerName, eventId);
        Duration ttl = Duration.ofDays(properties.getConsumptionDedupDays());
        Boolean success = redisOps.setIfAbsent(key, "1", ttl);
        if (Boolean.FALSE.equals(success)) {
            log.info("[OutboxIdempotent] duplicated eventId={} consumer={}", eventId, consumerName);
            return false;
        }
        return true;
    }
}
