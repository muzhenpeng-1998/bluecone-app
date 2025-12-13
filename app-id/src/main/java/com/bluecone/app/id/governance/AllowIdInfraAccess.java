package com.bluecone.app.id.governance;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 允许少量基础设施代码直接访问 ID 基础实现的豁免注解。
 *
 * <p>仅限于 app-id 自身或极少数桥接层使用，业务层禁止使用。</p>
 */
@Target({ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowIdInfraAccess {
}

