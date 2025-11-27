package com.bluecone.app.infra.cache.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis 封装的 L2 缓存，负责跨节点共享。
 */
public class L2CacheStore implements CacheStore {

    private static final Logger log = LoggerFactory.getLogger(L2CacheStore.class);
    private static final String NULL_MARKER = "NULL";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public L2CacheStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> Optional<T> get(CacheKey key, Class<T> type) {
        String payload = redisTemplate.opsForValue().get(key.toRedisKey());
        if (payload == null) {
            return Optional.empty();
        }
        if (NULL_MARKER.equals(payload)) {
            @SuppressWarnings("unchecked")
            T nullValue = (T) NullValue.INSTANCE;
            return Optional.of(nullValue);
        }
        try {
            return Optional.ofNullable(objectMapper.readValue(payload, type));
        } catch (JsonProcessingException ex) {
            log.warn("L2 cache decode failed, key={}, message={}", key, ex.getMessage());
            evict(key);
            return Optional.empty();
        }
    }

    @Override
    public void put(CacheKey key, Object value, Duration ttl) {
        String payload;
        if (value instanceof NullValue) {
            payload = NULL_MARKER;
        } else {
            try {
                payload = objectMapper.writeValueAsString(value);
            } catch (JsonProcessingException ex) {
                log.warn("L2 cache encode failed, skip put. key={}, message={}", key, ex.getMessage());
                return;
            }
        }
        redisTemplate.opsForValue().set(key.toRedisKey(), payload, ttl);
    }

    @Override
    public void evict(CacheKey key) {
        redisTemplate.delete(key.toRedisKey());
    }
}
