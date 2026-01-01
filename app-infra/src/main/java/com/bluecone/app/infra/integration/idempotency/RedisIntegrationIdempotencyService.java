package com.bluecone.app.infra.integration.idempotency;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;

/**
 * 基于 Redis 的集成幂等服务实现。
 * <p>
 * 使用 Redis SETNX + EXPIRE 实现分布式幂等检查。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class RedisIntegrationIdempotencyService implements IntegrationIdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(RedisIntegrationIdempotencyService.class);
    private static final String KEY_PREFIX = "idemp:integration:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean tryAcquire(String key, Duration ttl) {
        if (key == null || ttl == null) {
            log.warn("[IntegrationIdempotency] key or ttl is null");
            return false;
        }

        try {
            String redisKey = buildRedisKey(key);
            Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", ttl);
            
            if (Boolean.TRUE.equals(success)) {
                log.debug("[IntegrationIdempotency] 幂等 key 设置成功（首次调用）, key={}, ttl={}", 
                        maskKey(key), ttl);
                return true;
            } else {
                log.info("[IntegrationIdempotency] 幂等 key 已存在（重复调用）, key={}", maskKey(key));
                return false;
            }
        } catch (Exception e) {
            log.error("[IntegrationIdempotency] tryAcquire 异常, key={}", maskKey(key), e);
            // 异常时返回 false，避免重复处理
            return false;
        }
    }

    @Override
    public boolean exists(String key) {
        if (key == null) {
            return false;
        }

        try {
            String redisKey = buildRedisKey(key);
            Boolean exists = redisTemplate.hasKey(redisKey);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("[IntegrationIdempotency] exists 异常, key={}", maskKey(key), e);
            return false;
        }
    }

    @Override
    public void delete(String key) {
        if (key == null) {
            return;
        }

        try {
            String redisKey = buildRedisKey(key);
            redisTemplate.delete(redisKey);
            log.debug("[IntegrationIdempotency] 幂等 key 已删除, key={}", maskKey(key));
        } catch (Exception e) {
            log.error("[IntegrationIdempotency] delete 异常, key={}", maskKey(key), e);
        }
    }

    /**
     * 构造 Redis key。
     * <p>
     * 格式：idemp:integration:sha256(key)
     * 使用 SHA256 哈希避免 key 过长，并保护敏感信息。
     * </p>
     */
    private String buildRedisKey(String key) {
        String hash = sha256(key);
        return KEY_PREFIX + hash;
    }

    /**
     * 计算 SHA256 哈希
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            log.error("[IntegrationIdempotency] SHA256 计算失败", e);
            // 降级：使用原始 key
            return input;
        }
    }

    /**
     * 脱敏 key（只显示前 16 个字符）
     */
    private String maskKey(String key) {
        if (key == null) {
            return "null";
        }
        if (key.length() <= 16) {
            return key.substring(0, Math.min(8, key.length())) + "***";
        }
        return key.substring(0, 16) + "***";
    }
}

