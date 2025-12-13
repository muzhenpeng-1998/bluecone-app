package com.bluecone.app.infra.contextkit;

import com.bluecone.app.core.contextkit.CacheKey;
import com.bluecone.app.core.contextkit.CacheValue;
import com.bluecone.app.core.contextkit.ContextCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

/**
 * 基于 Redis 的通用上下文缓存实现（L2，可选）。
 */
public class RedisContextCache implements ContextCache {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisContextCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> Optional<CacheValue> get(CacheKey key) {
        String payload = redisTemplate.opsForValue().get(toRedisKey(key));
        if (payload == null) {
            return Optional.empty();
        }
        try {
            CacheValue value = objectMapper.readValue(payload, CacheValue.class);
            return Optional.ofNullable(value);
        } catch (Exception ex) {
            // 解析失败直接删除，避免污染
            redisTemplate.delete(toRedisKey(key));
            return Optional.empty();
        }
    }

    @Override
    public void put(CacheKey key, CacheValue value, Duration ttl) {
        try {
            String payload = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(toRedisKey(key), payload, ttl);
        } catch (Exception ex) {
            // 序列化失败时跳过写入
        }
    }

    @Override
    public void invalidate(CacheKey key) {
        redisTemplate.delete(toRedisKey(key));
    }

    private String toRedisKey(CacheKey key) {
        return key.namespace() + ":" + key.key();
    }
}

