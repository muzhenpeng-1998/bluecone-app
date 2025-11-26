package com.bluecone.app.core.log.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiLog {

    String value() default "";

    boolean logOnSuccess() default true;

    boolean logOnError() default true;

    LogLevel successLevel() default LogLevel.INFO;

    LogLevel bizErrorLevel() default LogLevel.WARN;

    LogLevel sysErrorLevel() default LogLevel.ERROR;

    boolean printStackTrace() default false;

    boolean logArgs() default true;

    boolean logResult() default true;

    boolean maskSensitive() default true;

    enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}
