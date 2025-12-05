package com.bluecone.app.security.session;

import com.bluecone.app.infra.security.session.AuthSessionEntity;
import com.bluecone.app.infra.security.session.AuthSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 基于 bc_auth_session 的简单会话管理实现。
 */
@Service
@RequiredArgsConstructor
public class AuthSessionManagerImpl implements AuthSessionManager {

    private static final long ACCESS_TOKEN_TTL_SECONDS = 2 * 60 * 60;
    private static final long REFRESH_TOKEN_TTL_SECONDS = 7 * 24 * 60 * 60;

    private final AuthSessionMapper authSessionMapper;

    @Override
    public AuthSessionCreateResult createSession(Long userId,
                                                 Long tenantId,
                                                 String clientType,
                                                 String deviceId,
                                                 String ipAddress,
                                                 String userAgent) {
        String accessToken = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime refreshExpireAt = now.plusSeconds(REFRESH_TOKEN_TTL_SECONDS);

        AuthSessionEntity entity = new AuthSessionEntity();
        entity.setId(accessToken);
        entity.setUserId(userId);
        entity.setTenantId(tenantId);
        entity.setClientType(clientType);
        entity.setDeviceId(deviceId);
        entity.setRefreshTokenHash(refreshToken);
        entity.setRefreshTokenExpireAt(refreshExpireAt);
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setLastActiveAt(now);
        // TODO: 保存 ipAddress/userAgent 等字段（需要表结构支持时扩展）
        authSessionMapper.insert(entity);

        long expireAtEpoch = Instant.now().plusSeconds(ACCESS_TOKEN_TTL_SECONDS).getEpochSecond();
        // TODO: 替换 UUID token 为 JWT/签名机制，并配置过期时间
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
