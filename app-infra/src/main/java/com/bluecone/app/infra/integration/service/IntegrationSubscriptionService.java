package com.bluecone.app.infra.integration.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.infra.integration.config.IntegrationProperties;
import com.bluecone.app.infra.integration.entity.IntegrationSubscriptionEntity;
import com.bluecone.app.infra.integration.mapper.IntegrationSubscriptionMapper;
import com.bluecone.app.infra.integration.support.IntegrationKeyBuilder;
import com.bluecone.app.core.tenant.TenantContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

/**
 * 订阅查询/缓存服务，多租户 + 平台级订阅组合。
 */
@Service
public class IntegrationSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationSubscriptionService.class);

    private final IntegrationSubscriptionMapper mapper;
    private final IntegrationKeyBuilder keyBuilder;
    private final Cache<String, List<IntegrationSubscriptionEntity>> cache;

    public IntegrationSubscriptionService(final IntegrationSubscriptionMapper mapper,
                                          final IntegrationKeyBuilder keyBuilder,
                                          final IntegrationProperties properties) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.keyBuilder = Objects.requireNonNull(keyBuilder, "keyBuilder must not be null");
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(properties.getSubscriptionCacheTtlSeconds()))
                .maximumSize(1000)
                .build();
    }

    /**
     * 查询指定租户的订阅，自动合并平台级（tenantId=0）。
     */
    public List<IntegrationSubscriptionEntity> findSubscriptions(final Long tenantId, final String eventType) {
        String cacheKey = keyBuilder.subscriptionCacheKey(tenantId, eventType);
        return cache.get(cacheKey, key -> loadSubscriptions(tenantId, eventType));
    }

    /**
     * 精确查询订阅。
     */
    public IntegrationSubscriptionEntity findById(final Long id, final Long tenantId) {
        String previousTenant = TenantContext.getTenantId();
        try {
            if (tenantId != null) {
                TenantContext.setTenantId(String.valueOf(tenantId));
            }
            return mapper.selectById(id);
        } finally {
            restoreTenant(previousTenant);
        }
    }

    /**
     * 手动刷新某租户相关的缓存。
     */
    public void refreshCacheForTenant(final Long tenantId) {
        ConcurrentMap<String, List<IntegrationSubscriptionEntity>> map = cache.asMap();
        String suffix = ":" + tenantId;
        map.keySet().removeIf(key -> key.contains(suffix));
    }

    private List<IntegrationSubscriptionEntity> loadSubscriptions(final Long tenantId, final String eventType) {
        List<IntegrationSubscriptionEntity> result = new ArrayList<>();
        if (tenantId != null) {
            result.addAll(queryByTenant(tenantId, eventType));
            // 平台级订阅：tenantId=0
            if (!Long.valueOf(0L).equals(tenantId)) {
                result.addAll(queryByTenant(0L, eventType));
            }
        } else {
            // 无租户上下文时，仍尝试平台级订阅
            result.addAll(queryByTenant(0L, eventType));
        }
        return result;
    }

    private List<IntegrationSubscriptionEntity> queryByTenant(final Long tenantId, final String eventType) {
        String previousTenant = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(String.valueOf(tenantId));
            LambdaQueryWrapper<IntegrationSubscriptionEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(IntegrationSubscriptionEntity::getEventType, eventType)
                    .eq(IntegrationSubscriptionEntity::getEnabled, Boolean.TRUE);
            return mapper.selectList(wrapper);
        } finally {
            restoreTenant(previousTenant);
        }
    }

    private void restoreTenant(final String previousTenant) {
        if (previousTenant == null) {
            TenantContext.clear();
        } else {
            TenantContext.setTenantId(previousTenant);
        }
    }
}
