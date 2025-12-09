package com.bluecone.app.resource.infrastructure.session;

import com.bluecone.app.infra.redis.core.RedisOps;
import com.bluecone.app.resource.api.enums.ResourceOwnerType;
import com.bluecone.app.resource.api.enums.ResourceProfileCode;
import com.bluecone.app.resource.api.enums.ResourcePurpose;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 上传会话存储，将上传状态临时保存在 Redis。
 */
@Component
public class UploadSessionStore {

    private static final String KEY_PREFIX = "res:upload:";

    private final RedisOps redisOps;
    private final ObjectMapper objectMapper;

    public UploadSessionStore(RedisOps redisOps, ObjectMapper objectMapper) {
        this.redisOps = redisOps;
        this.objectMapper = objectMapper;
    }

    public void save(UploadSession session, Duration ttl) {
        try {
            String payload = objectMapper.writeValueAsString(session);
            redisOps.setString(key(session.uploadToken()), payload, ttl);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("无法序列化上传会话", ex);
        }
    }

    public Optional<UploadSession> load(String uploadToken) {
        String payload = redisOps.getString(key(uploadToken));
        if (payload == null) {
            return Optional.empty();
        }
        try {
            UploadSession session = objectMapper.readValue(payload, UploadSession.class);
            return Optional.of(session);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("无法反序列化上传会话", ex);
        }
    }

    public void delete(String uploadToken) {
        redisOps.delete(key(uploadToken));
    }

    private static String key(String token) {
        return KEY_PREFIX + token;
    }

    /**
     * 上传会话元信息，包含预期大小、用途等。
     */
    public static record UploadSession(String uploadToken,
                                       Long tenantId,
                                       ResourceProfileCode profileCode,
                                       ResourceOwnerType ownerType,
                                       Long ownerId,
                                       Long expectedSize,
                                       String expectedHash,
                                       String storageKey,
                                       String contentType,
                                       ResourcePurpose purpose,
                                       Instant expiresAt) {
    }
}
