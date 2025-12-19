package com.bluecone.app.api.advice;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记不需要被 ApiResponse 自动包装的方法或类。
 * <p>用于支付/微信回调等需要返回原始格式的接口。</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoApiResponseWrap {
}
