package com.bluecone.app.gateway.middleware;

import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.ApiMiddleware;
import com.bluecone.app.gateway.ApiMiddlewareChain;
import com.bluecone.app.infra.redis.core.RedisKeyBuilder;
import com.bluecone.app.infra.redis.core.RedisKeyNamespace;
import com.bluecone.app.infra.redis.core.RedisOps;

import lombok.RequiredArgsConstructor;

/**
 * Simple fixed-window rate limiter based on Redis.
 */
@Component
@RequiredArgsConstructor
public class RateLimitMiddleware implements ApiMiddleware {

    private final RedisOps redisOps;
    private final RedisKeyBuilder redisKeyBuilder;

    @Override
    public void apply(ApiContext ctx, ApiMiddlewareChain chain) throws Exception {
        if (!ctx.getContract().isRateLimitEnabled()) {
            chain.next(ctx);
            return;
        }

        String key = buildKey(ctx);
        Duration window = ctx.getContract().getRateLimitWindow();
        int capacity = ctx.getContract().getRateLimitCapacity();

        Long current = redisOps.incr(key, 1);
        if (current != null && current == 1) {
            redisOps.expire(key, window);
        }
        if (current != null && current > capacity) {
            throw BusinessException.of(ErrorCode.RATE_LIMITED.getCode(), "Too many requests");
        }
        chain.next(ctx);
    }

    private String buildKey(ApiContext ctx) {
        String bizId = StringUtils.hasText(ctx.getTenantId()) ? ctx.getTenantId() : "global";
        String rateLimitKey = StringUtils.hasText(ctx.getContract().getRateLimitKey())
                ? ctx.getContract().getRateLimitKey()
                : ctx.getContract().getCode();
        return redisKeyBuilder.build(RedisKeyNamespace.RATE_LIMIT, bizId, rateLimitKey);
    }
}
