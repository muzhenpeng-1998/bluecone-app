package com.bluecone.app.infra.cache.annotation;

import com.bluecone.app.infra.cache.profile.CacheProfileName;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 声明式缓存失效注解，用于 DB 更新后广播失效。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheEvict {

    CacheProfileName profile();

    @AliasFor("key")
    String value() default "";

    @AliasFor("value")
    String key() default "";
}
