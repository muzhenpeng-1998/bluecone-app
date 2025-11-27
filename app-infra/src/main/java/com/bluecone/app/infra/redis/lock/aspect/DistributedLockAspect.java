package com.bluecone.app.infra.redis.lock.aspect;

import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.infra.redis.lock.DistributedLock;
import com.bluecone.app.infra.redis.lock.LockProperties;
import com.bluecone.app.infra.redis.lock.annotation.DistributedLock.FailStrategy;
import com.bluecone.app.infra.redis.support.MethodInvocationContext;
import com.bluecone.app.infra.redis.support.SpelExpressionEvaluator;

/**
 * 分布式锁切面：在方法执行前获取锁，执行完成后释放。
 * <p>设计要点：统一使用 SpEL 生成业务 key，所有 Redis 访问委托给 DistributedLock 实现。</p>
 */
@Aspect
@Component
@EnableConfigurationProperties(LockProperties.class)
public class DistributedLockAspect {

    private static final String DEFAULT_LOCK_ERROR_MESSAGE = "Failed to acquire distributed lock";

    private final LockProperties properties;
    private final DistributedLock distributedLock;
    private final SpelExpressionEvaluator spelExpressionEvaluator;

    public DistributedLockAspect(LockProperties properties,
                                 DistributedLock distributedLock,
                                 SpelExpressionEvaluator spelExpressionEvaluator) {
        this.properties = properties;
        this.distributedLock = distributedLock;
        this.spelExpressionEvaluator = spelExpressionEvaluator;
    }

    /**
     * 在方法执行前尝试获取分布式锁，依据策略决定失败行为。
     *
     * @param joinPoint      切点
     * @param lockAnnotation 注解配置
     * @return 方法执行结果或策略定义的返回
     * @throws Throwable 原方法抛出的异常
     */
    @Around("@annotation(lockAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint,
                         com.bluecone.app.infra.redis.lock.annotation.DistributedLock lockAnnotation) throws Throwable {
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }
        MethodInvocationContext context = MethodInvocationContext.from(joinPoint);
        String bizKey = spelExpressionEvaluator.evaluateToString(lockAnnotation.key(), context);
        if (!StringUtils.hasText(bizKey)) {
            throw new IllegalArgumentException("DistributedLock key SpEL evaluated to blank for method " + context.method());
        }
        long waitTimeMs = resolveTime(lockAnnotation.waitTime(), lockAnnotation.timeUnit().toMillis(1), properties.getDefaultWaitTimeMs());
        long leaseTimeMs = resolveTime(lockAnnotation.leaseTime(), lockAnnotation.timeUnit().toMillis(1), properties.getDefaultLeaseTimeMs());
        String ownerId = buildOwnerId();
        boolean locked = distributedLock.tryLock(bizKey, ownerId, waitTimeMs, leaseTimeMs);
        if (!locked) {
            return handleAcquireFailure(lockAnnotation.failStrategy(), bizKey);
        }
        try {
            return joinPoint.proceed();
        } finally {
            distributedLock.unlock(bizKey, ownerId);
        }
    }

    private Object handleAcquireFailure(FailStrategy strategy, String bizKey) {
        return switch (strategy) {
            case SKIP -> null;
            case CUSTOM_CODE -> throw new IllegalStateException("Custom fail strategy not implemented for key: " + bizKey);
            case THROW -> throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(),
                    DEFAULT_LOCK_ERROR_MESSAGE + ": " + bizKey);
        };
    }

    private long resolveTime(long annotationValue, long unitMultiplier, long defaultValueMs) {
        if (annotationValue > -1) {
            return annotationValue * unitMultiplier;
        }
        return defaultValueMs;
    }

    private String buildOwnerId() {
        return "node-" + UUID.randomUUID() + "-thread-" + Thread.currentThread().getId();
    }
}
