package com.bluecone.app.infra.redis.lock.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 声明式分布式锁注解，基于 SpEL 生成业务键，自动完成加锁/解锁。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * 业务键 SpEL 表达式，例如 "'order:create:' + #request.clientOrderId"。
     */
    String key();

    /**
     * 等待时长，默认使用全局配置。
     */
    long waitTime() default -1L;

    /**
     * 租约时长，默认使用全局配置。
     */
    long leaseTime() default -1L;

    /**
     * 时间单位，默认毫秒。
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    /**
     * 获取失败时的处理策略。
     */
    FailStrategy failStrategy() default FailStrategy.THROW;

    /**
     * 失败策略定义。
     */
    enum FailStrategy {
        /**
         * 抛业务异常。
         */
        THROW,
        /**
         * 直接跳过方法执行，返回 null，谨慎使用。
         */
        SKIP,
        /**
         * 预留自定义处理扩展。
         */
        CUSTOM_CODE
    }
}
