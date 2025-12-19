package com.bluecone.app.security.admin;

import java.lang.annotation.*;

/**
 * 后台权限校验注解
 * 
 * 用于Controller方法上，声明该接口需要的权限。
 * 支持单个权限或多个权限的AND/OR关系校验。
 * 
 * 示例：
 * <pre>
 * // 单个权限
 * {@code @RequireAdminPermission("product:edit")}
 * 
 * // 多个权限（AND关系，必须全部拥有）
 * {@code @RequireAdminPermission(value = {"product:edit", "product:online"}, requireAll = true)}
 * 
 * // 多个权限（OR关系，拥有任一即可）
 * {@code @RequireAdminPermission(value = {"product:view", "product:edit"}, requireAll = false)}
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAdminPermission {
    
    /**
     * 权限代码
     * 
     * 格式：{resource}:{action}
     * 例如：product:edit, order:view, store:manage
     */
    String[] value();
    
    /**
     * 是否要求拥有所有权限（AND关系）
     * 
     * true: 必须拥有所有权限（AND）
     * false: 拥有任一权限即可（OR）
     * 
     * 默认：true
     */
    boolean requireAll() default true;
    
    /**
     * 权限描述（可选，用于文档生成）
     */
    String description() default "";
}
