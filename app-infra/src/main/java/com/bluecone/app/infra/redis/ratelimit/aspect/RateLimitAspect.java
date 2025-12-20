package com.bluecone.app.infra.redis.ratelimit.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.infra.redis.ratelimit.RateLimitKeyResolver;
import com.bluecone.app.infra.redis.ratelimit.RateLimitProperties;
import com.bluecone.app.infra.redis.ratelimit.RateLimitStrategy;
import com.bluecone.app.infra.redis.ratelimit.RedisRateLimiter;
import com.bluecone.app.infra.redis.support.MethodInvocationContext;

/**
 * 限流切面：在方法执行前检查限流窗口，按策略处理溢出请求。
 */
@Aspect
@Component
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitAspect {

    private final RateLimitProperties properties;
    private final RedisRateLimiter redisRateLimiter;
    private final RateLimitKeyResolver keyResolver;

    public RateLimitAspect(RateLimitProperties properties,
                           RedisRateLimiter redisRateLimiter,
                           RateLimitKeyResolver keyResolver) {
        this.properties = properties;
        this.redisRateLimiter = redisRateLimiter;
        this.keyResolver = keyResolver;
    }

    /**
     * 在方法执行前检查限流窗口，超限时按策略处理。
     *
     * @param joinPoint 切点
     * @param rateLimit 注解配置
     * @return 原方法返回或策略定义的结果
     * @throws Throwable 原方法异常
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint,
                         com.bluecone.app.infra.redis.ratelimit.annotation.RateLimit rateLimit) throws Throwable {
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }
        MethodInvocationContext context = MethodInvocationContext.from(joinPoint);
        String bizKey = keyResolver.resolve(rateLimit.key(), context);
        int limit = rateLimit.limit() > -1 ? rateLimit.limit() : properties.getDefaultLimit();
        int windowSeconds = rateLimit.windowSeconds() > -1 ? rateLimit.windowSeconds() : properties.getDefaultWindowSeconds();
        boolean allowed = redisRateLimiter.tryAcquire(bizKey, limit, windowSeconds);
        if (allowed) {
            return joinPoint.proceed();
        }
        return handleOverflow(rateLimit.strategy(), bizKey);
    }

    private Object handleOverflow(RateLimitStrategy strategy, String bizKey) {
        return switch (strategy) {
            case SILENT_DROP -> null;
            case FALLBACK -> throw new IllegalStateException("Fallback strategy not implemented for key: " + bizKey);
            case REJECT -> throw new BusinessException(CommonErrorCode.SYSTEM_ERROR,
                    "Too many requests for key: " + bizKey);
        };
    }
}
