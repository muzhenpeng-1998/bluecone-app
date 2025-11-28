package com.bluecone.app.api.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bluecone.app.api.auth.dto.LoginRequest;
import com.bluecone.app.api.auth.dto.LoginResponse;
import com.bluecone.app.api.auth.dto.LogoutAllRequest;
import com.bluecone.app.api.auth.dto.LogoutRequest;
import com.bluecone.app.api.auth.dto.RefreshTokenRequest;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.infra.security.session.AuthSessionEntity;
import com.bluecone.app.infra.security.session.AuthSessionService;
import com.bluecone.app.infra.security.token.TokenProperties;
import com.bluecone.app.infra.security.token.TokenProvider;
import com.bluecone.app.infra.security.token.TokenUserContext;
import com.bluecone.app.infra.security.token.blacklist.TokenBlacklistService;
import com.bluecone.app.security.core.SecurityConstants;
import com.bluecone.app.security.core.SecurityUserPrincipal;
import com.bluecone.app.user.domain.UserEntity;
import com.bluecone.app.user.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 认证相关 API。
 */
@RestController
@RequestMapping("/api/auth")
@Validated
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final TokenProvider tokenProvider;
    private final TokenProperties tokenProperties;
    private final AuthSessionService authSessionService;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        UserEntity user = userService.validateAndGetUser(request.getUsername(), request.getPassword());
        if (user.getTenantId() != null) {
            com.bluecone.app.infra.tenant.TenantContext.setTenantId(String.valueOf(user.getTenantId()));
        }
        String sessionId = UUID.randomUUID().toString();
        TokenUserContext context = TokenUserContext.builder()
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .sessionId(sessionId)
                .clientType(request.getClientType())
                .deviceId(request.getDeviceId())
                .build();

        String refreshToken = tokenProvider.generateRefreshToken(context);
        authSessionService.createSessionOnLogin(sessionId, user.getId(), user.getTenantId(),
                request.getClientType(), request.getDeviceId(), refreshToken,
                Duration.ofDays(tokenProperties.getRefreshTokenTtlDays()));

        String accessToken = tokenProvider.generateAccessToken(context);
        LocalDateTime accessExpireAt = LocalDateTime.now().plusMinutes(tokenProperties.getAccessTokenTtlMinutes());
        LocalDateTime refreshExpireAt = LocalDateTime.now().plusDays(tokenProperties.getRefreshTokenTtlDays());

        log.info("User login success, userId={}, tenantId={}, clientType={}, deviceId={}, sessionId={}",
                user.getId(), user.getTenantId(), request.getClientType(), request.getDeviceId(), sessionId);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpireAt(accessExpireAt)
                .refreshTokenExpireAt(refreshExpireAt)
                .sessionId(sessionId)
                .build();
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenUserContext ctx = tokenProvider.parseRefreshToken(request.getRefreshToken());
        if (ctx.getTenantId() != null) {
            com.bluecone.app.infra.tenant.TenantContext.setTenantId(String.valueOf(ctx.getTenantId()));
        }
        AuthSessionEntity session = authSessionService.getActiveSession(ctx.getSessionId());
        if (!hash(request.getRefreshToken()).equals(session.getRefreshTokenHash())) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID.getCode(),
                    ErrorCode.AUTH_TOKEN_INVALID.getMessage());
        }
        if (session.getRefreshTokenExpireAt() != null && session.getRefreshTokenExpireAt().isBefore(LocalDateTime.now())) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID.getCode(),
                    ErrorCode.AUTH_TOKEN_INVALID.getMessage());
        }

        TokenUserContext accessCtx = TokenUserContext.builder()
                .userId(ctx.getUserId())
                .tenantId(ctx.getTenantId())
                .sessionId(ctx.getSessionId())
                .clientType(ctx.getClientType())
                .deviceId(ctx.getDeviceId())
                .build();
        String newAccessToken = tokenProvider.generateAccessToken(accessCtx);
        authSessionService.refreshLastActive(ctx.getSessionId());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .accessTokenExpireAt(LocalDateTime.now().plusMinutes(tokenProperties.getAccessTokenTtlMinutes()))
                .refreshTokenExpireAt(session.getRefreshTokenExpireAt())
                .sessionId(ctx.getSessionId())
                .build();
    }

    @PostMapping("/logout")
    public void logout(@RequestBody(required = false) LogoutRequest request, HttpServletRequest servletRequest) {
        String accessToken = resolveBearer(servletRequest);
        TokenUserContext ctx = StringUtils.hasText(accessToken) ? tokenProvider.parseAccessToken(accessToken) : null;
        if (ctx != null && ctx.getTenantId() != null) {
            com.bluecone.app.infra.tenant.TenantContext.setTenantId(String.valueOf(ctx.getTenantId()));
        }
        String sessionId = request != null && StringUtils.hasText(request.getSessionId())
                ? request.getSessionId()
                : (ctx != null ? ctx.getSessionId() : null);
        if (!StringUtils.hasText(sessionId)) {
            throw BusinessException.of(ErrorCode.AUTH_SESSION_INVALID.getCode(),
                    ErrorCode.AUTH_SESSION_INVALID.getMessage());
        }
        if (ctx != null && ctx.getExpireAt() != null) {
            Duration ttl = Duration.between(LocalDateTime.now(), ctx.getExpireAt());
            if (!ttl.isNegative() && !ttl.isZero()) {
                tokenBlacklistService.blacklistAccessToken(ctx.getTokenId(), ttl);
            }
        }
        authSessionService.markSessionRevoked(sessionId);
        log.info("Logout success, sessionId={}, userId={}, tenantId={}", sessionId,
                ctx != null ? ctx.getUserId() : null,
                ctx != null ? ctx.getTenantId() : null);
    }

    @PostMapping("/logout-all")
    public void logoutAll(@RequestBody(required = false) LogoutAllRequest request) {
        SecurityUserPrincipal principal = currentPrincipal();
        if (principal == null) {
            throw BusinessException.of(ErrorCode.AUTH_SESSION_INVALID.getCode(),
                    ErrorCode.AUTH_SESSION_INVALID.getMessage());
        }
        authSessionService.revokeAllByUser(principal.getUserId(), principal.getTenantId(),
                request != null ? request.getClientType() : null);
        log.info("Logout all, userId={}, tenantId={}, clientType={}", principal.getUserId(),
                principal.getTenantId(), request != null ? request.getClientType() : null);
    }

    private SecurityUserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUserPrincipal)) {
            return null;
        }
        return (SecurityUserPrincipal) authentication.getPrincipal();
    }

    private String resolveBearer(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.AUTH_HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return null;
        }
        return header.substring(SecurityConstants.BEARER_PREFIX.length()).trim();
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(encoded.length * 2);
            for (byte b : encoded) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
