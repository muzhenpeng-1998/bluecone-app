package com.bluecone.app.gateway.middleware;

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
 * Enforces idempotency using Redis SETNX.
 */
@Component
@RequiredArgsConstructor
public class IdempotentMiddleware implements ApiMiddleware {

    private final RedisOps redisOps;
    private final RedisKeyBuilder redisKeyBuilder;

    @Override
    public void apply(ApiContext ctx, ApiMiddlewareChain chain) throws Exception {
        if (!ctx.getContract().isIdempotent()) {
            chain.next(ctx);
            return;
        }
        String keyExpression = ctx.getContract().getIdempotentKeyExpression();
        String rawKey = resolveKey(ctx, keyExpression);
        if (!StringUtils.hasText(rawKey)) {
            throw BusinessException.of(ErrorCode.IDEMPOTENT_REJECTED.getCode(),
                    "Idempotent key missing");
        }
        String key = redisKeyBuilder.build(RedisKeyNamespace.IDEMPOTENT,
                StringUtils.hasText(ctx.getTenantId()) ? ctx.getTenantId() : "global",
                rawKey);
        Boolean locked = redisOps.setIfAbsent(key, "1", ctx.getContract().getIdempotentTtl());
        if (Boolean.FALSE.equals(locked)) {
            throw BusinessException.of(ErrorCode.IDEMPOTENT_REJECTED.getCode(),
                    "Duplicate request detected");
        }
        chain.next(ctx);
    }

    private String resolveKey(ApiContext ctx, String expression) {
        if (!StringUtils.hasText(expression) || ctx.getRequest() == null) {
            return expression;
        }
        if (expression.startsWith("header:")) {
            String headerName = expression.substring("header:".length());
            return ctx.getRequest().getHeader(headerName);
        }
        if (expression.startsWith("query:")) {
            String param = expression.substring("query:".length());
            return ctx.getRequest().getParameter(param);
        }
        if (expression.startsWith("path:")) {
            String variable = expression.substring("path:".length());
            return ctx.getPathVariables().get(variable);
        }
        return expression;
    }
}
