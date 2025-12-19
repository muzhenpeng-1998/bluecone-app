package com.bluecone.app.security.session;

import com.bluecone.app.infra.security.session.AuthSessionEntity;
import com.bluecone.app.infra.security.session.AuthSessionMapper;
import com.bluecone.app.infra.security.token.TokenProvider;
import com.bluecone.app.infra.security.token.TokenUserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 基于 bc_auth_session 的简单会话管理实现。
 */
@Service
@RequiredArgsConstructor
public class AuthSessionManagerImpl implements AuthSessionManager {

    private static final long REFRESH_TOKEN_TTL_SECONDS = 7 * 24 * 60 * 60;

    private final AuthSessionMapper authSessionMapper;
    private final TokenProvider tokenProvider;

    @Override
    public AuthSessionCreateResult createSession(Long userId,
                                                 Long tenantId,
                                                 String clientType,
                                                 String deviceId,
                                                 String ipAddress,
                                                 String userAgent) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime refreshExpireAt = now.plusSeconds(REFRESH_TOKEN_TTL_SECONDS);

        // 生成会话 ID（用于在数据库中唯一标识这个会话）
        String sessionId = UUID.randomUUID().toString();

        // 保存会话到数据库
        AuthSessionEntity entity = new AuthSessionEntity();
        entity.setId(sessionId);
        entity.setUserId(userId);
        entity.setTenantId(tenantId);
        entity.setClientType(clientType);
        entity.setDeviceId(deviceId);
        entity.setRefreshTokenHash(UUID.randomUUID().toString()); // 临时占位，后续生成真实 refresh token
        entity.setRefreshTokenExpireAt(refreshExpireAt);
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setLastActiveAt(now);
        // TODO: 保存 ipAddress/userAgent 等字段（需要表结构支持时扩展）
        authSessionMapper.insert(entity);

        // 使用 TokenProvider 生成 JWT access token
        TokenUserContext tokenUserContext = TokenUserContext.builder()
                .userId(userId)
                .tenantId(tenantId)
                .sessionId(sessionId)
                .clientType(clientType)
                .deviceId(deviceId)
                .build();
        String accessToken = tokenProvider.generateAccessToken(tokenUserContext);
        String refreshToken = tokenProvider.generateRefreshToken(tokenUserContext);

        // 计算过期时间戳（用于返回给客户端）
        long expireAtEpoch = Instant.now().plus(Duration.ofMinutes(15)).getEpochSecond(); // 默认 15 分钟
        
        return new AuthSessionCreateResult(accessToken, refreshToken, expireAtEpoch, userId, tenantId);
    }

    @Override
    public AuthSessionCreateResult refreshSession(String refreshToken) {
        throw new UnsupportedOperationException("TODO: implement refreshSession");
    }

    @Override
    public void revokeSession(String accessToken) {
        throw new UnsupportedOperationException("TODO: implement revokeSession");
    }
}
