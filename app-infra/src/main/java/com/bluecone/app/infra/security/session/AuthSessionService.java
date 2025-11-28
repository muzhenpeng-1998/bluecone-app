package com.bluecone.app.infra.security.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.infra.security.session.cache.AuthSessionSnapshot;
import com.bluecone.app.infra.security.session.cache.SessionCacheService;

import lombok.RequiredArgsConstructor;

/**
 * 会话管理服务，负责创建、校验与注销。
 */
@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private static final Logger log = LoggerFactory.getLogger(AuthSessionService.class);

    private final AuthSessionMapper authSessionMapper;
    private final SessionCacheService sessionCacheService;

    /**
     * 登录后创建新会话。
     */
    @Transactional
    public AuthSessionEntity createSessionOnLogin(String sessionId, Long userId, Long tenantId, String clientType,
                                                  String deviceId, String refreshToken, Duration refreshTtl) {
        LocalDateTime now = LocalDateTime.now();
        AuthSessionEntity entity = new AuthSessionEntity();
        entity.setId(StringUtils.hasText(sessionId) ? sessionId : UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setTenantId(tenantId);
        entity.setClientType(clientType);
        entity.setDeviceId(deviceId);
        entity.setRefreshTokenHash(hash(refreshToken));
        entity.setRefreshTokenExpireAt(now.plus(refreshTtl));
        entity.setStatus(AuthSessionStatus.ACTIVE.name());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setLastActiveAt(now);

        authSessionMapper.insert(entity);
        cacheSnapshot(entity, refreshTtl);
        log.info("Login session created, userId={}, tenantId={}, clientType={}, deviceId={}, sessionId={}",
                userId, tenantId, clientType, deviceId, entity.getId());
        return entity;
    }

    /**
     * 基于刷新 token 刷新会话信息（可用于旋转 refresh token）。
     */
    @Transactional
    public AuthSessionEntity refreshSessionOnRefreshToken(String sessionId, String newRefreshToken, Duration refreshTtl) {
        AuthSessionEntity session = requireActiveSession(sessionId);
        LocalDateTime now = LocalDateTime.now();
        session.setRefreshTokenHash(hash(newRefreshToken));
        session.setRefreshTokenExpireAt(now.plus(refreshTtl));
        session.setUpdatedAt(now);
        session.setLastActiveAt(now);
        authSessionMapper.updateById(session);
        cacheSnapshot(session, refreshTtl);
        return session;
    }

    /**
     * 获取活跃会话并校验状态/过期。
     */
    public AuthSessionEntity getActiveSession(String sessionId) {
        return requireActiveSession(sessionId);
    }

    /**
     * 标记单个会话为 revoked。
     */
    @Transactional
    public void markSessionRevoked(String sessionId) {
        AuthSessionEntity session = authSessionMapper.selectById(sessionId);
        if (session == null) {
            return;
        }
        session.setStatus(AuthSessionStatus.REVOKED.name());
        session.setUpdatedAt(LocalDateTime.now());
        authSessionMapper.updateById(session);
        sessionCacheService.evictSession(sessionId);
        log.info("Session revoked, sessionId={}, userId={}, tenantId={}", sessionId, session.getUserId(), session.getTenantId());
    }

    /**
     * 按用户批量注销会话，可按客户端类型过滤。
     */
    @Transactional
    public void revokeAllByUser(Long userId, Long tenantId, String clientType) {
        LocalDateTime now = LocalDateTime.now();
        List<AuthSessionEntity> sessions = authSessionMapper.selectList(new LambdaQueryWrapper<AuthSessionEntity>()
                .eq(AuthSessionEntity::getUserId, userId)
                .eq(AuthSessionEntity::getTenantId, tenantId)
                .eq(StringUtils.hasText(clientType), AuthSessionEntity::getClientType, clientType)
                .eq(AuthSessionEntity::getStatus, AuthSessionStatus.ACTIVE.name()));
        if (CollectionUtils.isEmpty(sessions)) {
            return;
        }
        List<String> sessionIds = sessions.stream().map(AuthSessionEntity::getId).toList();
        authSessionMapper.update(null, new LambdaUpdateWrapper<AuthSessionEntity>()
                .set(AuthSessionEntity::getStatus, AuthSessionStatus.REVOKED.name())
                .set(AuthSessionEntity::getUpdatedAt, now)
                .in(AuthSessionEntity::getId, sessionIds));
        sessionIds.forEach(sessionCacheService::evictSession);
        log.info("Revoked sessions for userId={}, tenantId={}, clientType={}, count={}", userId, tenantId, clientType, sessionIds.size());
    }

    /**
     * 刷新活跃时间。
     */
    public void refreshLastActive(String sessionId) {
        AuthSessionEntity session = authSessionMapper.selectById(sessionId);
        if (session == null) {
            return;
        }
        session.setLastActiveAt(LocalDateTime.now());
        session.setUpdatedAt(session.getLastActiveAt());
        authSessionMapper.updateById(session);
        cacheSnapshot(session, computeTtl(session));
    }

    private AuthSessionEntity requireActiveSession(String sessionId) {
        AuthSessionEntity session = authSessionMapper.selectById(sessionId);
        if (session == null) {
            throw BusinessException.of(ErrorCode.AUTH_SESSION_INVALID.getCode(),
                    ErrorCode.AUTH_SESSION_INVALID.getMessage());
        }
        if (AuthSessionStatus.REVOKED.name().equals(session.getStatus())) {
            throw BusinessException.of(ErrorCode.AUTH_SESSION_INVALID.getCode(),
                    ErrorCode.AUTH_SESSION_INVALID.getMessage());
        }
        if (session.getRefreshTokenExpireAt() != null && session.getRefreshTokenExpireAt().isBefore(LocalDateTime.now())) {
            markExpired(session);
            throw BusinessException.of(ErrorCode.AUTH_SESSION_INVALID.getCode(),
                    ErrorCode.AUTH_SESSION_INVALID.getMessage());
        }
        cacheSnapshot(session, computeTtl(session));
        return session;
    }

    private void markExpired(AuthSessionEntity session) {
        session.setStatus(AuthSessionStatus.EXPIRED.name());
        session.setUpdatedAt(LocalDateTime.now());
        authSessionMapper.updateById(session);
        sessionCacheService.evictSession(session.getId());
    }

    private void cacheSnapshot(AuthSessionEntity entity, Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            return;
        }
        AuthSessionSnapshot snapshot = AuthSessionSnapshot.builder()
                .sessionId(entity.getId())
                .userId(entity.getUserId())
                .tenantId(entity.getTenantId())
                .clientType(entity.getClientType())
                .deviceId(entity.getDeviceId())
                .status(AuthSessionStatus.valueOf(entity.getStatus()))
                .refreshTokenExpireAt(entity.getRefreshTokenExpireAt())
                .lastActiveAt(entity.getLastActiveAt())
                .build();
        sessionCacheService.putSession(snapshot, ttl);
    }

    private Duration computeTtl(AuthSessionEntity entity) {
        if (entity.getRefreshTokenExpireAt() == null) {
            return null;
        }
        return Duration.between(LocalDateTime.now(), entity.getRefreshTokenExpireAt());
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
