package com.bluecone.app.infra.cache.annotation;

import com.bluecone.app.infra.cache.profile.CacheProfileName;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 声明式缓存命中注解，聚焦“用哪个 Profile + 业务 key”。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cached {

    CacheProfileName profile();

    @AliasFor("key")
    String value() default "";

    @AliasFor("value")
    String key() default "";
}
