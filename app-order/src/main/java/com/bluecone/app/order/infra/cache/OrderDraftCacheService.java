package com.bluecone.app.order.infra.cache;

import com.bluecone.app.infra.redis.core.RedisOps;
import com.bluecone.app.order.api.cart.dto.OrderDraftViewDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 订单草稿多级缓存（Caffeine + Redis）门面。
 */
@Service
public class OrderDraftCacheService {

    private static final Duration REDIS_TTL = Duration.ofMinutes(30);
    private static final long L1_TTL_SECONDS = 60;

    private final RedisOps redisOps;
    private final ObjectMapper objectMapper;
    private final Cache<String, OrderDraftViewDTO> l1Cache;

    public OrderDraftCacheService(RedisOps redisOps, ObjectMapper objectMapper) {
        this.redisOps = redisOps;
        this.objectMapper = objectMapper;
        this.l1Cache = Caffeine.newBuilder()
                .expireAfterWrite(L1_TTL_SECONDS, TimeUnit.SECONDS)
                .maximumSize(10_000)
                .build();
    }

    /**
     * 读取缓存：先查 Caffeine，miss 后查 Redis，命中即回填 Caffeine。
     */
    public OrderDraftViewDTO getFromCache(Long tenantId,
                                          Long storeId,
                                          Long userId,
                                          String channel,
                                          String scene) {
        String key = OrderDraftCacheKeys.buildDraftKey(tenantId, storeId, userId, channel, scene);
        OrderDraftViewDTO l1 = l1Cache.getIfPresent(key);
        if (l1 != null) {
            return l1;
        }
        String json = redisOps.getString(key);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        OrderDraftViewDTO view = deserialize(json);
        if (view != null) {
            l1Cache.put(key, view);
        }
        return view;
    }

    /**
     * 写缓存：更新 Redis + Caffeine。
     */
    public void putToCache(Long tenantId,
                           Long storeId,
                           Long userId,
                           String channel,
                           String scene,
                           OrderDraftViewDTO view) {
        if (view == null) {
            return;
        }
        String key = OrderDraftCacheKeys.buildDraftKey(tenantId, storeId, userId, channel, scene);
        String json = serialize(view);
        if (json != null) {
            redisOps.setString(key, json, REDIS_TTL);
            l1Cache.put(key, view);
        }
    }

    /**
     * 删除缓存：Caffeine + Redis。
     */
    public void evictCache(Long tenantId,
                           Long storeId,
                           Long userId,
                           String channel,
                           String scene) {
        String key = OrderDraftCacheKeys.buildDraftKey(tenantId, storeId, userId, channel, scene);
        l1Cache.invalidate(key);
        redisOps.delete(key);
    }

    private String serialize(OrderDraftViewDTO view) {
        try {
            return objectMapper.writeValueAsString(view);
        } catch (Exception e) {
            return null;
        }
    }

    private OrderDraftViewDTO deserialize(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return null;
        }
    }
}
