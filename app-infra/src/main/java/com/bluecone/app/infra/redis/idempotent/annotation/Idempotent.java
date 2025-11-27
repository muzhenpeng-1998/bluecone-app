package com.bluecone.app.infra.redis.idempotent.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.bluecone.app.infra.redis.idempotent.IdempotentScene;

/**
 * 声明式幂等注解，基于 Redis 存储请求状态防重复执行。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等业务键 SpEL 表达式。
     */
    String key();

    /**
     * 幂等场景标识。
     */
    IdempotentScene scene() default IdempotentScene.API;

    /**
     * 幂等标记过期时间（秒），默认使用全局配置。
     */
    int expireSeconds() default -1;
}
