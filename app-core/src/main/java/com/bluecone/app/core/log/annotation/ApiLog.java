package com.bluecone.app.core.log.annotation;

import java.lang.annotation.*;

/**
 * 事件驱动日志注解。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiLog {

    String value() default "";

    /**
     * 是否记录入参/出参摘要到 payload（仅调试场景）。
     */
    boolean includePayload() default false;
}
