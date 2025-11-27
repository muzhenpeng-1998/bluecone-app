package com.bluecone.app.infra.redis.support;

import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * 封装 AOP 切入点的调用信息，便于在能力层中统一处理参数与方法元数据。
 * <p>设计意图：让 SpEL 解析、日志或后续扩展都依赖这一轻量上下文，而不直接耦合 AOP API。</p>
 */
public final class MethodInvocationContext {

    private final Method method;
    private final Object[] args;
    private final Object target;
    private final Class<?> targetClass;

    private MethodInvocationContext(Method method, Object[] args, Object target, Class<?> targetClass) {
        this.method = method;
        this.args = args;
        this.target = target;
        this.targetClass = targetClass;
    }

    /**
     * 从 ProceedingJoinPoint 构造上下文，供环绕通知使用。
     *
     * @param joinPoint AOP 切点
     * @return 方法调用上下文
     */
    public static MethodInvocationContext from(ProceedingJoinPoint joinPoint) {
        return fromJoinPoint(joinPoint);
    }

    /**
     * 从 JoinPoint 构造上下文，适用于前置/后置通知等场景。
     *
     * @param joinPoint AOP 切点
     * @return 方法调用上下文
     */
    public static MethodInvocationContext from(JoinPoint joinPoint) {
        return fromJoinPoint(joinPoint);
    }

    private static MethodInvocationContext fromJoinPoint(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object target = joinPoint.getTarget();
        Class<?> targetClass = target != null ? target.getClass() : signature.getDeclaringType();
        return new MethodInvocationContext(method, joinPoint.getArgs(), target, targetClass);
    }

    /**
     * 被拦截的方法定义。
     *
     * @return Method 对象
     */
    public Method method() {
        return method;
    }

    /**
     * 方法入参列表。
     *
     * @return 原始参数数组
     */
    public Object[] args() {
        return args;
    }

    /**
     * 被拦截的目标对象实例。
     *
     * @return 目标对象
     */
    public Object target() {
        return target;
    }

    /**
     * 目标类型，便于日志或调试。
     *
     * @return 目标 Class
     */
    public Class<?> targetClass() {
        return targetClass;
    }
}
