package com.bluecone.app.security.admin;

import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.infra.admin.service.AdminAuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 后台权限校验AOP拦截器
 * 
 * 拦截带有 @RequireAdminPermission 注解的方法，进行权限校验。
 * 
 * 校验逻辑：
 * 1. 从SecurityContext获取当前用户ID
 * 2. 从TenantContext获取当前租户ID
 * 3. 调用AdminAuthorizationService进行权限校验
 * 4. 校验失败抛出AccessDeniedException（403）
 */
@Slf4j
@Aspect
@Component
@Order(100) // 确保在事务之后执行
@RequiredArgsConstructor
public class AdminPermissionAspect {
    
    private final AdminAuthorizationService authorizationService;
    
    @Around("@annotation(com.bluecone.app.security.admin.RequireAdminPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireAdminPermission annotation = method.getAnnotation(RequireAdminPermission.class);
        
        String[] requiredPermissions = annotation.value();
        boolean requireAll = annotation.requireAll();
        
        // 2. 获取当前用户和租户信息
        Long userId = getCurrentUserId();
        Long tenantId = getCurrentTenantId();
        
        if (userId == null) {
            log.warn("权限校验失败：用户未登录, method={}", method.getName());
            throw new AccessDeniedException("用户未登录");
        }
        
        if (tenantId == null) {
            log.warn("权限校验失败：租户上下文缺失, userId={}, method={}", userId, method.getName());
            throw new AccessDeniedException("租户上下文缺失");
        }
        
        // 3. 权限校验
        boolean hasPermission;
        if (requireAll) {
            hasPermission = authorizationService.hasAllPermissions(tenantId, userId, requiredPermissions);
        } else {
            hasPermission = authorizationService.hasAnyPermission(tenantId, userId, requiredPermissions);
        }
        
        if (!hasPermission) {
            log.warn("权限校验失败：权限不足, userId={}, tenantId={}, requiredPermissions={}, requireAll={}, method={}", 
                    userId, tenantId, requiredPermissions, requireAll, method.getName());
            throw new AccessDeniedException("权限不足：" + String.join(", ", requiredPermissions));
        }
        
        log.debug("权限校验通过: userId={}, tenantId={}, permissions={}", userId, tenantId, requiredPermissions);
        
        // 4. 执行目标方法
        return joinPoint.proceed();
    }
    
    /**
     * 从SecurityContext获取当前用户ID
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() != null) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof com.bluecone.app.security.core.SecurityUserPrincipal) {
                    return ((com.bluecone.app.security.core.SecurityUserPrincipal) principal).getUserId();
                }
            }
        } catch (Exception e) {
            log.error("获取当前用户ID失败", e);
        }
        
        // 尝试从请求头获取（兼容性处理）
        try {
            ServletRequestAttributes attributes = 
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userIdHeader = request.getHeader("X-User-Id");
                if (userIdHeader != null) {
                    return Long.parseLong(userIdHeader);
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        
        return null;
    }
    
    /**
     * 从TenantContext获取当前租户ID
     */
    private Long getCurrentTenantId() {
        try {
            String tenantIdStr = TenantContext.getTenantId();
            if (tenantIdStr != null) {
                return Long.parseLong(tenantIdStr);
            }
        } catch (Exception e) {
            log.error("获取当前租户ID失败", e);
        }
        
        // 尝试从请求头获取（兼容性处理）
        try {
            ServletRequestAttributes attributes = 
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String tenantIdHeader = request.getHeader("X-Tenant-Id");
                if (tenantIdHeader != null) {
                    return Long.parseLong(tenantIdHeader);
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        
        return null;
    }
}
