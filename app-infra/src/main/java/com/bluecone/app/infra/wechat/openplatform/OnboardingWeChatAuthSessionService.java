package com.bluecone.app.infra.wechat.openplatform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 微信开放平台授权会话服务。
 * <p>
 * 用于生成预授权 URL 时创建 state 会话，并在授权回调时验证 state。
 * 会话存储在 Redis 中，TTL 为 10 分钟。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class OnboardingWeChatAuthSessionService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingWeChatAuthSessionService.class);
    private static final String KEY_PREFIX = "wechat:auth:session:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 创建授权会话并返回 state。
     *
     * @param tenantId 租户 ID
     * @param storeId  门店 ID（可选）
     * @return state（UUID）
     */
    public String createSession(Long tenantId, Long storeId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId 不能为空");
        }

        String state = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusMinutes(10);

        OnboardingWeChatAuthSession session = OnboardingWeChatAuthSession.builder()
                .state(state)
                .tenantId(tenantId)
                .storeId(storeId)
                .createdAt(now)
                .expireAt(expireAt)
                .build();

        try {
            String json = objectMapper.writeValueAsString(session);
            String redisKey = buildRedisKey(state);
            redisTemplate.opsForValue().set(redisKey, json, SESSION_TTL);

            log.info("[OnboardingWeChatAuthSession] 创建授权会话, state={}, tenantId={}, storeId={}",
                    maskState(state), tenantId, storeId);

            return state;

        } catch (JsonProcessingException e) {
            log.error("[OnboardingWeChatAuthSession] 序列化会话失败", e);
            throw new IllegalStateException("创建授权会话失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据 state 获取授权会话。
     *
     * @param state state
     * @return 授权会话，如果不存在或已过期返回 Optional.empty()
     */
    public Optional<OnboardingWeChatAuthSession> getSession(String state) {
        if (state == null || state.isBlank()) {
            log.warn("[OnboardingWeChatAuthSession] state 为空");
            return Optional.empty();
        }

        try {
            String redisKey = buildRedisKey(state);
            String json = redisTemplate.opsForValue().get(redisKey);

            if (json == null) {
                log.warn("[OnboardingWeChatAuthSession] 会话不存在或已过期, state={}", maskState(state));
                return Optional.empty();
            }

            OnboardingWeChatAuthSession session = objectMapper.readValue(json, OnboardingWeChatAuthSession.class);

            log.info("[OnboardingWeChatAuthSession] 获取授权会话, state={}, tenantId={}, storeId={}",
                    maskState(state), session.getTenantId(), session.getStoreId());

            return Optional.of(session);

        } catch (Exception e) {
            log.error("[OnboardingWeChatAuthSession] 反序列化会话失败, state={}", maskState(state), e);
            return Optional.empty();
        }
    }

    /**
     * 删除授权会话（授权成功后调用）。
     *
     * @param state state
     */
    public void deleteSession(String state) {
        if (state == null || state.isBlank()) {
            return;
        }

        try {
            String redisKey = buildRedisKey(state);
            redisTemplate.delete(redisKey);

            log.info("[OnboardingWeChatAuthSession] 删除授权会话, state={}", maskState(state));

        } catch (Exception e) {
            log.error("[OnboardingWeChatAuthSession] 删除会话失败, state={}", maskState(state), e);
        }
    }

    /**
     * 构造 Redis key
     */
    private String buildRedisKey(String state) {
        return KEY_PREFIX + state;
    }

    /**
     * 脱敏 state（只显示前 8 个字符）
     */
    private String maskState(String state) {
        if (state == null || state.length() <= 8) {
            return "***";
        }
        return state.substring(0, 8) + "***";
    }
}

