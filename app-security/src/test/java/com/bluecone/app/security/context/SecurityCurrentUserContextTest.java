package com.bluecone.app.security.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import com.bluecone.app.core.context.CurrentUserContext;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.security.core.SecurityUserPrincipal;

/**
 * Unit tests for SecurityCurrentUserContext.
 */
class SecurityCurrentUserContextTest {

    private SecurityCurrentUserContext currentUserContext;

    @BeforeEach
    void setUp() {
        currentUserContext = new SecurityCurrentUserContext();
        // 清空 SecurityContext
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetCurrentUserId_whenNoAuthentication_shouldThrowAuthRequired() {
        // given: 无 authentication
        SecurityContextHolder.clearContext();

        // when & then: 抛出 AUTH_REQUIRED 异常
        BusinessException exception = assertThrows(BusinessException.class, 
                () -> currentUserContext.getCurrentUserId());
        assertEquals(ErrorCode.AUTH_REQUIRED.getCode(), exception.getCode());
    }

    @Test
    void testGetCurrentUserId_whenAnonymousUser_shouldThrowAuthRequired() {
        // given: authentication 的 principal 为 anonymousUser
        SecurityContext context = new SecurityContextImpl();
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken("anonymousUser", null);
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // when & then: 抛出 AUTH_REQUIRED 异常
        BusinessException exception = assertThrows(BusinessException.class, 
                () -> currentUserContext.getCurrentUserId());
        assertEquals(ErrorCode.AUTH_REQUIRED.getCode(), exception.getCode());
    }

    @Test
    void testGetCurrentUserId_whenNullPrincipal_shouldThrowAuthRequired() {
        // given: authentication 的 principal 为 null
        SecurityContext context = new SecurityContextImpl();
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(null, null);
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // when & then: 抛出 AUTH_REQUIRED 异常
        BusinessException exception = assertThrows(BusinessException.class, 
                () -> currentUserContext.getCurrentUserId());
        assertEquals(ErrorCode.AUTH_REQUIRED.getCode(), exception.getCode());
    }

    @Test
    void testGetCurrentUserId_whenStringPrincipal_shouldThrowAuthRequired() {
        // given: authentication 的 principal 为 String（不是 SecurityUserPrincipal）
        SecurityContext context = new SecurityContextImpl();
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken("someuser", null);
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // when & then: 抛出 AUTH_REQUIRED 异常
        BusinessException exception = assertThrows(BusinessException.class, 
                () -> currentUserContext.getCurrentUserId());
        assertEquals(ErrorCode.AUTH_REQUIRED.getCode(), exception.getCode());
    }

    @Test
    void testGetCurrentUserId_whenValidSecurityUserPrincipal_shouldReturnUserId() {
        // given: authentication 的 principal 为有效的 SecurityUserPrincipal
        Long expectedUserId = 1001L;
        Long expectedTenantId = 2001L;
        SecurityUserPrincipal principal = new SecurityUserPrincipal(
                expectedUserId, expectedTenantId, "testuser", "web", "device123");
        
        SecurityContext context = new SecurityContextImpl();
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // when
        Long userId = currentUserContext.getCurrentUserId();

        // then
        assertEquals(expectedUserId, userId);
    }

    @Test
    void testGetCurrentTenantId_whenValidSecurityUserPrincipal_shouldReturnTenantId() {
        // given: authentication 的 principal 为有效的 SecurityUserPrincipal
        Long expectedUserId = 1001L;
        Long expectedTenantId = 2001L;
        SecurityUserPrincipal principal = new SecurityUserPrincipal(
                expectedUserId, expectedTenantId, "testuser", "web", "device123");
        
        SecurityContext context = new SecurityContextImpl();
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // when
        Long tenantId = currentUserContext.getCurrentTenantId();

        // then
        assertEquals(expectedTenantId, tenantId);
    }

    @Test
    void testGetCurrentMemberIdOrNull_shouldReturnNull() {
        // given: authentication 的 principal 为有效的 SecurityUserPrincipal
        // SecurityUserPrincipal 当前不包含 memberId 字段
        Long expectedUserId = 1001L;
        Long expectedTenantId = 2001L;
        SecurityUserPrincipal principal = new SecurityUserPrincipal(
                expectedUserId, expectedTenantId, "testuser", "web", "device123");
        
        SecurityContext context = new SecurityContextImpl();
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // when
        Long memberId = currentUserContext.getCurrentMemberIdOrNull();

        // then: 当前实现始终返回 null
        assertNull(memberId);
    }
}
