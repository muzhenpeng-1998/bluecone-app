package com.bluecone.app.security.jwt;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
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
import com.bluecone.app.security.core.SecurityUserPrincipal;
import com.bluecone.app.security.handler.RestAuthenticationEntryPoint;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * JWT 认证过滤器。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuthSessionService authSessionService;
    private final SessionCacheService sessionCacheService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String bearerToken = resolveToken(request);
            if (!StringUtils.hasText(bearerToken)) {
                filterChain.doFilter(request, response);
                return;
            }

            TokenUserContext tokenUserContext = tokenProvider.parseAccessToken(bearerToken);
            if (tokenUserContext.getTenantId() != null) {
                TenantContext.setTenantId(String.valueOf(tokenUserContext.getTenantId()));
            }
            ensureTokenNotBlacklisted(tokenUserContext);
            ensureSessionValid(tokenUserContext);

            SecurityUserPrincipal principal = new SecurityUserPrincipal(
                    tokenUserContext.getUserId(),
                    tokenUserContext.getTenantId(),
                    String.valueOf(tokenUserContext.getUserId()),
                    tokenUserContext.getClientType(),
                    tokenUserContext.getDeviceId());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, bearerToken, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (BusinessException ex) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response,
                    new InsufficientAuthenticationException(ex.getMessage(), ex));
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response,
                    new InsufficientAuthenticationException("Authentication failed", ex));
        }
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

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.AUTH_HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return null;
        }
        return header.substring(SecurityConstants.BEARER_PREFIX.length()).trim();
    }
}
