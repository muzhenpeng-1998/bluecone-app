package com.bluecone.app.billing.guard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * PlanGuard AOP 切面
 * 通过注解方式简化权限检查
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PlanGuardAspect {
    
    private final PlanGuard planGuard;
    
    /**
     * 拦截 @RequireWritePermission 注解
     */
    @Around("@annotation(requireWritePermission)")
    public Object checkWritePermission(ProceedingJoinPoint joinPoint, RequireWritePermission requireWritePermission) throws Throwable {
        // 从方法参数中获取 tenantId
        Long tenantId = extractTenantId(joinPoint);
        
        if (tenantId != null) {
            String operation = requireWritePermission.value();
            if (operation.isEmpty()) {
                operation = joinPoint.getSignature().getName();
            }
            
            planGuard.checkWritePermission(tenantId, operation);
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * 拦截 @RequireAdvancedFeature 注解
     */
    @Around("@annotation(requireAdvancedFeature)")
    public Object checkAdvancedFeature(ProceedingJoinPoint joinPoint, RequireAdvancedFeature requireAdvancedFeature) throws Throwable {
        // 从方法参数中获取 tenantId
        Long tenantId = extractTenantId(joinPoint);
        
        if (tenantId != null) {
            planGuard.checkAdvancedFeature(tenantId, requireAdvancedFeature.value());
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * 从方法参数中提取 tenantId
     * 假设第一个 Long 类型参数是 tenantId，或者参数名为 tenantId
     */
    private Long extractTenantId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        
        // 尝试通过参数名查找
        for (int i = 0; i < parameterNames.length; i++) {
            if ("tenantId".equals(parameterNames[i]) && args[i] instanceof Long) {
                return (Long) args[i];
            }
        }
        
        // 如果没有找到，返回第一个 Long 类型参数
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }
        
        return null;
    }
    
    /**
     * 需要写权限注解
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RequireWritePermission {
        String value() default "";
    }
    
    /**
     * 需要高级功能注解
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RequireAdvancedFeature {
        String value();
    }
}
