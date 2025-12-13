package com.bluecone.app.infra.idresolve;

import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bluecone.app.core.idresolve.spi.PublicIdL2Cache;
import com.bluecone.app.core.idresolve.spi.PublicIdL2CacheResult;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.id.core.Ulid128;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 基于 Redis 的 L2 公共 ID 缓存实现。
 *
 * <p>Key：bc:pid:{tenantId}:{resourceType}:{publicId}</p>
 * <p>Value：内部 ULID128 的 16 字节 Base64 表示；负缓存使用常量标记。</p>
 */
@Component
public class RedisPublicIdL2Cache implements PublicIdL2Cache {

    private static final String NULL_MARKER = "NULL";

    private final StringRedisTemplate redisTemplate;

    public RedisPublicIdL2Cache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public PublicIdL2CacheResult get(long tenantId, ResourceType type, String publicId) {
        String key = buildKey(tenantId, type, publicId);
        String payload = redisTemplate.opsForValue().get(key);
        if (payload == null) {
            return PublicIdL2CacheResult.miss();
        }
        if (NULL_MARKER.equals(payload)) {
            return PublicIdL2CacheResult.negativeHit();
        }
        return PublicIdL2CacheResult.positiveHit(decodeUlid(payload));
    }

    @Override
    public Map<String, PublicIdL2CacheResult> getBatch(long tenantId, ResourceType type, List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return Map.of();
        }
        Map<String, String> keyByPublicId = new HashMap<>(publicIds.size());
        Map<String, String> publicIdByKey = new HashMap<>(publicIds.size());
        for (String publicId : publicIds) {
            String key = buildKey(tenantId, type, publicId);
            keyByPublicId.put(publicId, key);
            publicIdByKey.put(key, publicId);
        }
        List<String> keys = List.copyOf(keyByPublicId.values());
        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        Map<String, PublicIdL2CacheResult> result = new HashMap<>();
        if (values == null) {
            return result;
        }
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = values.get(i);
            if (value == null) {
                continue;
            }
            String publicId = publicIdByKey.get(key);
            if (publicId == null) {
                continue;
            }
            if (NULL_MARKER.equals(value)) {
                result.put(publicId, PublicIdL2CacheResult.negativeHit());
            } else {
                result.put(publicId, PublicIdL2CacheResult.positiveHit(decodeUlid(value)));
            }
        }
        return result;
    }

    @Override
    public void putPositive(long tenantId, ResourceType type, String publicId, Ulid128 internalId, Duration ttl) {
        String key = buildKey(tenantId, type, publicId);
        String payload = encodeUlid(internalId);
        redisTemplate.opsForValue().set(key, payload, ttl);
    }

    @Override
    public void putNegative(long tenantId, ResourceType type, String publicId, Duration ttl) {
        String key = buildKey(tenantId, type, publicId);
        redisTemplate.opsForValue().set(key, NULL_MARKER, ttl);
    }

    private String buildKey(long tenantId, ResourceType type, String publicId) {
        return "bc:pid:" + tenantId + ":" + type.name() + ":" + publicId;
    }

    private String encodeUlid(Ulid128 id) {
        return Base64.getEncoder().encodeToString(id.toBytes());
    }

    private Ulid128 decodeUlid(String payload) {
        byte[] bytes = Base64.getDecoder().decode(payload);
        return Ulid128.fromBytes(bytes);
    }
}
