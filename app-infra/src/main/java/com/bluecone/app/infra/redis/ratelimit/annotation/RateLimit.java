package com.bluecone.app.infra.redis.ratelimit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.bluecone.app.infra.redis.ratelimit.RateLimitStrategy;

/**
 * 声明式接口限流注解。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 业务键 SpEL 表达式。
     */
    String key();

    /**
     * 窗口内允许的最大调用次数，未指定时使用全局默认值。
     */
    int limit() default -1;

    /**
     * 窗口大小（秒），未指定时使用全局默认值。
     */
    int windowSeconds() default -1;

    /**
     * 限流触发策略。
     */
    RateLimitStrategy strategy() default RateLimitStrategy.REJECT;
}
