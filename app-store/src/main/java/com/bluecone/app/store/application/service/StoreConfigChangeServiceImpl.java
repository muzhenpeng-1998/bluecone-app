package com.bluecone.app.store.application.service;

import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.event.EventMetadata;
import com.bluecone.app.infra.cache.core.CacheKey;
import com.bluecone.app.infra.cache.facade.CacheClient;
import com.bluecone.app.infra.cache.profile.CacheProfileName;
import com.bluecone.app.infra.cache.profile.CacheProfileRegistry;
import com.bluecone.app.store.application.service.StoreConfigService;
import com.bluecone.app.store.event.StoreConfigChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 店铺配置变更通知服务的基础实现。
 * <p>职责：统一做缓存失效并发布配置变更事件。</p>
 */
@Service
public class StoreConfigChangeServiceImpl implements StoreConfigChangeService {

    private static final Logger log = LoggerFactory.getLogger(StoreConfigChangeServiceImpl.class);

    private final CacheClient cacheClient;
    private final CacheProfileRegistry cacheProfileRegistry;
    private final StoreConfigService storeConfigService;
    private final DomainEventPublisher eventPublisher;

    public StoreConfigChangeServiceImpl(CacheClient cacheClient,
                                        CacheProfileRegistry cacheProfileRegistry,
                                        StoreConfigService storeConfigService,
                                        DomainEventPublisher eventPublisher) {
        this.cacheClient = cacheClient;
        this.cacheProfileRegistry = cacheProfileRegistry;
        this.storeConfigService = storeConfigService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onStoreConfigChanged(Long tenantId, Long storeId, Long newConfigVersion) {
        if (tenantId == null || storeId == null) {
            log.warn("[StoreConfigChanged] skip due to missing identifier tenantId={} storeId={}", tenantId, storeId);
            return;
        }

        log.info("[StoreConfigChanged] tenantId={}, storeId={}, configVersion={}", tenantId, storeId, newConfigVersion);

        // 失效视图级缓存：store_base & store_snapshot（按 storeId 维度，避免继续复用旧版本聚合）
        String tenantKey = String.valueOf(tenantId);
        String baseBizId = tenantId + ":" + storeId;
        evict(CacheProfileName.STORE_BASE, tenantKey, baseBizId);

        // snapshot 需要区分渠道；若无法枚举渠道，至少删除空渠道 key 并记录日志
        evict(CacheProfileName.STORE_SNAPSHOT, tenantKey, baseBizId + ":");
        resolveChannelTypes(tenantId, storeId, newConfigVersion).forEach(channel ->
                evict(CacheProfileName.STORE_SNAPSHOT, tenantKey, baseBizId + ":" + channel));

        // 发布领域事件，供下游同步/缓存刷新/多级缓存等
        EventMetadata metadata = buildMetadata(tenantId);
        eventPublisher.publish(new StoreConfigChangedEvent(tenantId, storeId, newConfigVersion, metadata));
    }

    private void evict(CacheProfileName profileName, String tenantKey, String bizId) {
        CacheKey key = CacheKey.generic(tenantKey, cacheProfileRegistry.getProfile(profileName).domain(), bizId);
        cacheClient.evict(profileName, key);
    }

    private EventMetadata buildMetadata(Long tenantId) {
        Map<String, String> meta = new HashMap<>();
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            meta.put("traceId", traceId);
        }
        if (tenantId != null) {
            meta.put("tenantId", tenantId.toString());
        }
        return meta.isEmpty() ? EventMetadata.empty() : EventMetadata.of(meta);
    }

    private Iterable<String> resolveChannelTypes(Long tenantId, Long storeId, Long configVersion) {
        try {
            return Optional.ofNullable(storeConfigService.loadConfig(tenantId, storeId, configVersion))
                    .map(config -> Optional.ofNullable(config.getChannels()).orElseGet(java.util.List::of))
                    .orElseGet(java.util.List::of)
                    .stream()
                    .map(channel -> channel.getChannelType() == null ? "" : channel.getChannelType())
                    .distinct()
                    .toList();
        } catch (Exception ex) {
            log.warn("resolve channel types failed for tenantId={} storeId={}, skip snapshot eviction", tenantId, storeId, ex);
            return java.util.List.of();
        }
    }
}
