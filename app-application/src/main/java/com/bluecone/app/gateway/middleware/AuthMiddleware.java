package com.bluecone.app.gateway.middleware;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.ApiMiddleware;
import com.bluecone.app.gateway.ApiMiddlewareChain;
import com.bluecone.app.infra.security.session.AuthSessionEntity;
import com.bluecone.app.infra.security.session.AuthSessionService;
import com.bluecone.app.infra.security.session.AuthSessionStatus;
import com.bluecone.app.infra.security.session.cache.AuthSessionSnapshot;
import com.bluecone.app.infra.security.session.cache.SessionCacheService;
import com.bluecone.app.infra.security.token.TokenProvider;
import com.bluecone.app.infra.security.token.TokenUserContext;
import com.bluecone.app.infra.security.token.blacklist.TokenBlacklistService;
import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.security.core.SecurityConstants;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Authenticates request using existing JWT/session stack.
 */
@Component
@RequiredArgsConstructor
public class AuthMiddleware implements ApiMiddleware {

    private final TokenProvider tokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuthSessionService authSessionService;
    private final SessionCacheService sessionCacheService;

    @Override
    public void apply(ApiContext ctx, ApiMiddlewareChain chain) throws Exception {
        if (!ctx.getContract().isAuthRequired()) {
            chain.next(ctx);
            return;
        }
        String bearerToken = resolveToken(ctx.getRequest());
        if (!StringUtils.hasText(bearerToken)) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID.getCode(),
                    ErrorCode.AUTH_TOKEN_INVALID.getMessage());
        }
        TokenUserContext userContext = tokenProvider.parseAccessToken(bearerToken);
        ensureTokenNotBlacklisted(userContext);
        ensureSessionValid(userContext);
        if (userContext.getTenantId() != null) {
            TenantContext.setTenantId(String.valueOf(userContext.getTenantId()));
            ctx.setTenantId(String.valueOf(userContext.getTenantId()));
        }
        ctx.setUserId(userContext.getUserId());
        ctx.setClientType(userContext.getClientType());
        chain.next(ctx);
    }

    private String resolveToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String header = request.getHeader(SecurityConstants.AUTH_HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return null;
        }
        return header.substring(SecurityConstants.BEARER_PREFIX.length()).trim();
    }

    private void ensureTokenNotBlacklisted(TokenUserContext ctx) {
        if (tokenBlacklistService.isAccessTokenBlacklisted(ctx.getTokenId())) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID.getCode(),
                    ErrorCode.AUTH_TOKEN_INVALID.getMessage());
        }
    }

    private void ensureSessionValid(TokenUserContext ctx) {
        if (!StringUtils.hasText(ctx.getSessionId())) {
            throw BusinessException.of(ErrorCode.AUTH_SESSION_INVALID.getCode(),
                    ErrorCode.AUTH_SESSION_INVALID.getMessage());
        }

        AuthSessionSnapshot snapshot = sessionCacheService.getSession(ctx.getSessionId()).orElse(null);
        if (snapshot != null) {
            validateSnapshot(ctx, snapshot);
        } else {
            AuthSessionEntity session = authSessionService.getActiveSession(ctx.getSessionId());
            validateEntity(ctx, session);
        }
        authSessionService.refreshLastActive(ctx.getSessionId());
    }

    private void validateSnapshot(TokenUserContext ctx, AuthSessionSnapshot snapshot) {
        if (!AuthSessionStatus.ACTIVE.equals(snapshot.getStatus())) {
            throw BusinessException.of(ErrorCode.AUTH_SESSION_INVALID.getCode(),
                    ErrorCode.AUTH_SESSION_INVALID.getMessage());
        }
        if (snapshot.getRefreshTokenExpireAt() != null && snapshot.getRefreshTokenExpireAt().isBefore(LocalDateTime.now())) {
            throw BusinessException.of(ErrorCode.AUTH_SESSION_INVALID.getCode(),
                    ErrorCode.AUTH_SESSION_INVALID.getMessage());
        }
        if (!ctx.getUserId().equals(snapshot.getUserId())) {
            throw BusinessException.of(ErrorCode.AUTH_SESSION_INVALID.getCode(),
                    ErrorCode.AUTH_SESSION_INVALID.getMessage());
        }
    }

    private void validateEntity(TokenUserContext ctx, AuthSessionEntity entity) {
        if (!AuthSessionStatus.ACTIVE.name().equals(entity.getStatus())) {
            throw BusinessException.of(ErrorCode.AUTH_SESSION_INVALID.getCode(),
                    ErrorCode.AUTH_SESSION_INVALID.getMessage());
        }
        if (!ctx.getUserId().equals(entity.getUserId())) {
            throw BusinessException.of(ErrorCode.AUTH_SESSION_INVALID.getCode(),
                    ErrorCode.AUTH_SESSION_INVALID.getMessage());
        }
    }
}
