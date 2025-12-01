package com.bluecone.app.infra.notify.timeline;

import com.bluecone.app.infra.redis.core.RedisKeyBuilder;
import com.bluecone.app.infra.redis.core.RedisKeyNamespace;
import com.bluecone.app.infra.redis.core.RedisOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Objects;

/**
 * 幂等控制：在幂等窗口内相同 key 仅接受一次。
 */
public class NotifyDedupService {

    private static final Logger log = LoggerFactory.getLogger(NotifyDedupService.class);

    private final RedisOps redisOps;
    private final RedisKeyBuilder redisKeyBuilder;

    public NotifyDedupService(RedisOps redisOps, RedisKeyBuilder redisKeyBuilder) {
        this.redisOps = Objects.requireNonNull(redisOps, "redisOps must not be null");
        this.redisKeyBuilder = Objects.requireNonNull(redisKeyBuilder, "redisKeyBuilder must not be null");
    }

    public boolean tryMark(String idempotentKey, int windowMinutes) {
        if (!StringUtils.hasText(idempotentKey)) {
            return true;
        }
        String key = redisKeyBuilder.build(RedisKeyNamespace.NOTIFY, idempotentKey, "idem");
        Boolean success = redisOps.setIfAbsent(key, "1", Duration.ofMinutes(windowMinutes));
        boolean firstSeen = Boolean.TRUE.equals(success);
        if (!firstSeen) {
            log.info("[NotifyDedup] duplicate request key={} window={}m", key, windowMinutes);
        }
        return firstSeen;
    }
}
