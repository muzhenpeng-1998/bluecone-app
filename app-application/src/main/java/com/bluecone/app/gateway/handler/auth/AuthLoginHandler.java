package com.bluecone.app.gateway.handler.auth;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.bluecone.app.api.auth.dto.LoginRequest;
import com.bluecone.app.api.auth.dto.LoginResponse;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.ApiHandler;
import com.bluecone.app.infra.security.session.AuthSessionService;
import com.bluecone.app.infra.security.token.TokenProperties;
import com.bluecone.app.infra.security.token.TokenProvider;
import com.bluecone.app.infra.security.token.TokenUserContext;
import com.bluecone.app.user.domain.UserEntity;
import com.bluecone.app.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gateway-native login handler using existing auth stack.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthLoginHandler implements ApiHandler<LoginRequest, LoginResponse> {

    private final UserService userService;
    private final TokenProvider tokenProvider;
    private final TokenProperties tokenProperties;
    private final AuthSessionService authSessionService;

    @Override
    public LoginResponse handle(ApiContext ctx, LoginRequest request) {
        UserEntity user = userService.validateAndGetUser(request.getUsername(), request.getPassword());

        String sessionId = UUID.randomUUID().toString();
        TokenUserContext tokenUserContext = TokenUserContext.builder()
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .sessionId(sessionId)
                .clientType(request.getClientType())
                .deviceId(request.getDeviceId())
                .build();

        String refreshToken = tokenProvider.generateRefreshToken(tokenUserContext);
        authSessionService.createSessionOnLogin(sessionId, user.getId(), user.getTenantId(),
                request.getClientType(), request.getDeviceId(), refreshToken,
                Duration.ofDays(tokenProperties.getRefreshTokenTtlDays()));

        String accessToken = tokenProvider.generateAccessToken(tokenUserContext);
        LocalDateTime accessExpireAt = LocalDateTime.now().plusMinutes(tokenProperties.getAccessTokenTtlMinutes());
        LocalDateTime refreshExpireAt = LocalDateTime.now().plusDays(tokenProperties.getRefreshTokenTtlDays());

        log.info("Gateway login success, userId={}, tenantId={}, clientType={}, deviceId={}, sessionId={}",
                user.getId(), user.getTenantId(), request.getClientType(), request.getDeviceId(), sessionId);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpireAt(accessExpireAt)
                .refreshTokenExpireAt(refreshExpireAt)
                .sessionId(sessionId)
                .build();
    }
}
