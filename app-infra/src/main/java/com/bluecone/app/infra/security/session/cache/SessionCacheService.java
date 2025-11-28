package com.bluecone.app.infra.security.session.cache;

import java.time.Duration;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.bluecone.app.infra.redis.core.RedisKeyBuilder;
import com.bluecone.app.infra.redis.core.RedisKeyNamespace;
import com.bluecone.app.infra.redis.core.RedisOps;

import lombok.RequiredArgsConstructor;

/**
 * Redis 会话缓存，降低数据库压力。
 */
@Service
@RequiredArgsConstructor
public class SessionCacheService {

    private static final String SNAPSHOT_SUFFIX = "snapshot";

    private final RedisOps redisOps;
    private final RedisKeyBuilder redisKeyBuilder;

    public Optional<AuthSessionSnapshot> getSession(String sessionId) {
        String key = buildKey(sessionId);
        return Optional.ofNullable(redisOps.getObject(key, AuthSessionSnapshot.class));
    }

    public void putSession(AuthSessionSnapshot snapshot, Duration ttl) {
        String key = buildKey(snapshot.getSessionId());
        redisOps.setObject(key, snapshot, ttl);
    }

    public void evictSession(String sessionId) {
        redisOps.delete(buildKey(sessionId));
    }

    private String buildKey(String sessionId) {
        return redisKeyBuilder.build(RedisKeyNamespace.SESSION, sessionId, SNAPSHOT_SUFFIX);
    }
}
