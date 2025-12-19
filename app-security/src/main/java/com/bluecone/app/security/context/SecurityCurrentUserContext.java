package com.bluecone.app.security.context;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.bluecone.app.core.context.CurrentUserContext;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.security.core.SecurityUserPrincipal;

/**
 * 基于 Spring Security 的当前用户上下文实现。
 * 
 * 从 SecurityContextHolder 获取当前登录用户信息，未登录时抛出 AUTH_REQUIRED 异常。
 */
@Component
public class SecurityCurrentUserContext implements CurrentUserContext {

    @Override
    public Long getCurrentUserId() {
        SecurityUserPrincipal principal = getAuthenticatedPrincipal();
        return principal.getUserId();
    }

    @Override
    public Long getCurrentTenantId() {
        SecurityUserPrincipal principal = getAuthenticatedPrincipal();
        return principal.getTenantId();
    }

    @Override
    public Long getCurrentMemberIdOrNull() {
        // SecurityUserPrincipal 当前不包含 memberId 字段
        // 如果后续需要支持，需要在 SecurityUserPrincipal 中添加 memberId 字段
        return null;
    }

    /**
     * 获取已认证的用户主体。
     * 
     * @return SecurityUserPrincipal
     * @throws BusinessException 如果未登录或认证信息无效
     */
    private SecurityUserPrincipal getAuthenticatedPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw BusinessException.of(ErrorCode.AUTH_REQUIRED.getCode(), 
                    ErrorCode.AUTH_REQUIRED.getMessage());
        }
        
        Object principal = authentication.getPrincipal();
        
        // 检查是否为匿名用户
        if (principal == null || "anonymousUser".equals(principal)) {
            throw BusinessException.of(ErrorCode.AUTH_REQUIRED.getCode(), 
                    ErrorCode.AUTH_REQUIRED.getMessage());
        }
        
        // 检查 principal 类型
        if (!(principal instanceof SecurityUserPrincipal)) {
            throw BusinessException.of(ErrorCode.AUTH_REQUIRED.getCode(), 
                    ErrorCode.AUTH_REQUIRED.getMessage());
        }
        
        return (SecurityUserPrincipal) principal;
    }
}
