package com.bluecone.app.gateway.handler.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.bluecone.app.api.auth.dto.LoginResponse;
import com.bluecone.app.api.auth.dto.RefreshTokenRequest;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.ApiHandler;
import com.bluecone.app.infra.security.session.AuthSessionEntity;
import com.bluecone.app.infra.security.session.AuthSessionService;
import com.bluecone.app.infra.security.token.TokenProperties;
import com.bluecone.app.infra.security.token.TokenProvider;
import com.bluecone.app.infra.security.token.TokenUserContext;

import lombok.RequiredArgsConstructor;

/**
 * Refresh access token handler.
 */
@Component
@RequiredArgsConstructor
public class AuthRefreshHandler implements ApiHandler<RefreshTokenRequest, LoginResponse> {

    private final TokenProvider tokenProvider;
    private final TokenProperties tokenProperties;
    private final AuthSessionService authSessionService;

    @Override
    public LoginResponse handle(ApiContext ctx, RefreshTokenRequest request) {
        TokenUserContext refreshCtx = tokenProvider.parseRefreshToken(request.getRefreshToken());
        AuthSessionEntity session = authSessionService.getActiveSession(refreshCtx.getSessionId());
        if (!hash(request.getRefreshToken()).equals(session.getRefreshTokenHash())) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID.getCode(),
                    ErrorCode.AUTH_TOKEN_INVALID.getMessage());
        }
        if (session.getRefreshTokenExpireAt() != null && session.getRefreshTokenExpireAt().isBefore(LocalDateTime.now())) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID.getCode(),
                    ErrorCode.AUTH_TOKEN_INVALID.getMessage());
        }

        TokenUserContext accessCtx = TokenUserContext.builder()
                .userId(refreshCtx.getUserId())
                .tenantId(refreshCtx.getTenantId())
                .sessionId(refreshCtx.getSessionId())
                .clientType(refreshCtx.getClientType())
                .deviceId(refreshCtx.getDeviceId())
                .build();
        String newAccessToken = tokenProvider.generateAccessToken(accessCtx);
        authSessionService.refreshLastActive(refreshCtx.getSessionId());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .accessTokenExpireAt(LocalDateTime.now().plusMinutes(tokenProperties.getAccessTokenTtlMinutes()))
                .refreshTokenExpireAt(session.getRefreshTokenExpireAt())
                .sessionId(refreshCtx.getSessionId())
                .build();
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
