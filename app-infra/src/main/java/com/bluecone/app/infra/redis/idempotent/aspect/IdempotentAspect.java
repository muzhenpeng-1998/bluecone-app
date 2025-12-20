package com.bluecone.app.infra.redis.idempotent.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.infra.redis.idempotent.IdempotentKeyResolver;
import com.bluecone.app.infra.redis.idempotent.IdempotentProperties;
import com.bluecone.app.infra.redis.idempotent.RedisIdempotentExecutor;
import com.bluecone.app.infra.redis.idempotent.annotation.Idempotent;
import com.bluecone.app.infra.redis.support.MethodInvocationContext;

/**
 * 幂等切面：方法进入前占位，执行完成后更新状态，阻止重复提交。
 */
@Aspect
@Component
@EnableConfigurationProperties(IdempotentProperties.class)
public class IdempotentAspect {

    private final IdempotentProperties properties;
    private final IdempotentKeyResolver keyResolver;
    private final RedisIdempotentExecutor executor;

    public IdempotentAspect(IdempotentProperties properties,
                            IdempotentKeyResolver keyResolver,
                            RedisIdempotentExecutor executor) {
        this.properties = properties;
        this.keyResolver = keyResolver;
        this.executor = executor;
    }

    /**
     * 在方法执行前占位幂等键，若重复则直接拒绝。
     *
     * @param joinPoint  切点
     * @param idempotent 注解配置
     * @return 原方法返回结果
     * @throws Throwable 原方法异常
     */
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }
        MethodInvocationContext context = MethodInvocationContext.from(joinPoint);
        String bizKey = keyResolver.resolve(idempotent.key(), context);
        int expireSeconds = idempotent.expireSeconds() > -1
                ? idempotent.expireSeconds()
                : properties.getDefaultExpireSeconds();
        boolean entered = executor.tryEnter(bizKey, idempotent.scene(), expireSeconds);
        if (!entered) {
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR,
                    "Duplicate request detected for key: " + bizKey);
        }
        Object result = joinPoint.proceed();
        executor.onSuccess(bizKey, idempotent.scene(), expireSeconds);
        return result;
    }
}
