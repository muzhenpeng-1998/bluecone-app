package com.bluecone.app.core.idresolve.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.bluecone.app.id.api.ResourceType;

/**
 * 标记 Controller 参数需要从 publicId 自动解析为内部 ULID128。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResolvePublicId {

    /**
     * 该参数对应的资源类型。
     */
    ResourceType type();
}

