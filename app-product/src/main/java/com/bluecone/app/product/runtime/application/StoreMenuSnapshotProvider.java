package com.bluecone.app.product.runtime.application;

import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
import com.bluecone.app.core.contextkit.*;
import com.bluecone.app.product.runtime.model.StoreMenuSnapshotData;
import com.bluecone.app.product.runtime.repository.StoreMenuSnapshotCacheRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 门店菜单快照 Provider（Prompt 08 + 门店级 Epoch）。
 * <p>
 * 复用 ContextMiddlewareKit 通用能力：
 * <ul>
 *   <li>L1/L2 多级缓存</li>
 *   <li>版本校验窗口 + 采样</li>
 *   <li>负缓存防扫库</li>
 *   <li>Epoch Keying 支持（namespace 级缓存失效）</li>
 * </ul>
 * <p>
 * <b>缓存键格式：</b>{tenantId}:{epoch}:{scopeId}
 * <ul>
 *   <li>scopeId = {storeId}:{channel}:{orderScene}</li>
 *   <li>epoch 按门店隔离（namespace = "store:menu:snap:{storeId}"）</li>
 * </ul>
 * <p>
 * <b>门店级 Epoch 设计：</b>
 * <ul>
 *   <li>每个门店使用独立的 namespace：CacheNamespaces.STORE_MENU_SNAPSHOT + ":" + storeId</li>
 *   <li>修改某个门店的菜单时，只会 bump 该门店的 epoch，不影响同租户其他门店</li>
 *   <li>避免租户级全量失效，提升缓存命中率</li>
 * </ul>
 * <p>
 * 使用场景：
 * <ul>
 *   <li>Open API 查询门店菜单快照</li>
 *   <li>小程序/H5 拉取菜单数据</li>
 *   <li>高并发读场景，避免频繁查询数据库</li>
 * </ul>
 *
 * @author System
 * @since 2025-12-21
 */
@Service
@Slf4j
public class StoreMenuSnapshotProvider {

    private final StoreMenuSnapshotCacheRepository repository;
    private final ContextCache cache;
    private final VersionChecker versionChecker;
    private final ContextKitProperties kitProperties;
    private final SnapshotProvider<StoreMenuSnapshotData> delegate;
    private final SnapshotSerde<StoreMenuSnapshotData> serde;
    private final CacheEpochProvider epochProvider;

    public StoreMenuSnapshotProvider(
            StoreMenuSnapshotCacheRepository repository,
            ContextCache cache,
            VersionChecker versionChecker,
            ContextKitProperties kitProperties,
            ObjectMapper objectMapper,
            CacheEpochProvider epochProvider
    ) {
        this.repository = repository;
        this.cache = cache;
        this.versionChecker = versionChecker;
        this.kitProperties = kitProperties;
        this.delegate = new SnapshotProvider<>();
        this.serde = new StoreMenuSnapshotSerde(objectMapper);
        this.epochProvider = epochProvider;
    }

    /**
     * 获取或加载门店菜单快照。
     * <p>
     * 缓存策略：
     * <ol>
     *   <li>优先从 L1 缓存（Caffeine）读取</li>
     *   <li>L1 miss 后从 L2 缓存（Redis）读取</li>
     *   <li>L2 miss 后从数据库加载</li>
     *   <li>定期采样校验版本号，确保缓存一致性</li>
     * </ol>
     * <p>
     * <b>门店级 Epoch：</b>使用 "store:menu:snap:{storeId}" 作为 namespace，
     * 确保只有该门店的菜单变更才会触发该门店的缓存失效。
     *
     * @param tenantId   租户ID
     * @param storeId    门店ID
     * @param channel    渠道（ALL, DINE_IN, TAKEAWAY, DELIVERY, PICKUP）
     * @param orderScene 订单场景（DEFAULT, BREAKFAST, LUNCH, DINNER, NIGHT）
     * @return 菜单快照数据（包含 menu_json 和 version）
     */
    public Optional<StoreMenuSnapshotData> getOrLoad(
            Long tenantId,
            Long storeId,
            String channel,
            String orderScene
    ) {
        // 构建 scopeId：{storeId}:{channel}:{orderScene}
        String scopeId = buildScopeId(storeId, channel, orderScene);
        
        // 构建门店级 namespace：store:menu:snap:{storeId}
        // 这样每个门店有独立的 epoch，避免租户级全量失效
        String storeNamespace = CacheNamespaces.STORE_MENU_SNAPSHOT + ":" + storeId;
        
        SnapshotLoadKey loadKey = new SnapshotLoadKey(
                tenantId,
                storeNamespace,
                scopeId
        );

        log.debug("获取门店菜单快照: tenantId={}, storeId={}, channel={}, orderScene={}, namespace={}", 
                tenantId, storeId, channel, orderScene, storeNamespace);

        StoreMenuSnapshotData snapshot = delegate.getOrLoad(
                loadKey,
                repository,
                cache,
                versionChecker,
                serde,
                kitProperties,
                epochProvider
        );

        return Optional.ofNullable(snapshot);
    }

    /**
     * 构建 scopeId。
     * <p>
     * 格式：{storeId}:{channel}:{orderScene}
     */
    private String buildScopeId(Long storeId, String channel, String orderScene) {
        return storeId + ":" + channel + ":" + orderScene;
    }

    /**
     * 门店菜单快照序列化/反序列化器。
     */
    private static class StoreMenuSnapshotSerde implements SnapshotSerde<StoreMenuSnapshotData> {

        private final ObjectMapper objectMapper;

        public StoreMenuSnapshotSerde(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Object toCacheValue(StoreMenuSnapshotData data) {
            if (data == null) {
                return null;
            }
            try {
                return objectMapper.writeValueAsString(data);
            } catch (JsonProcessingException e) {
                log.error("序列化门店菜单快照失败", e);
                return null;
            }
        }

        @Override
        public StoreMenuSnapshotData fromCacheValue(Object cacheValue) {
            if (cacheValue == null) {
                return null;
            }
            if (cacheValue instanceof String json) {
                try {
                    return objectMapper.readValue(json, StoreMenuSnapshotData.class);
                } catch (JsonProcessingException e) {
                    log.error("反序列化门店菜单快照失败: {}", json, e);
                    return null;
                }
            }
            log.warn("不支持的缓存值类型: {}", cacheValue.getClass());
            return null;
        }
    }
}

