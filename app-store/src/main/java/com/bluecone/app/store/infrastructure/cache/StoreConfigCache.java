package com.bluecone.app.store.infrastructure.cache;

import com.bluecone.app.store.domain.model.StoreConfig;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 门店配置多级缓存扩展点，当前以内存 Map 作为占位实现。
 * <p>高并发：缓存 key 携带 configVersion，防止版本回退导致的脏读。</p>
 * <p>高稳定：后续可接入本地缓存 + Redis，支持降级（Redis 异常回退本地）。</p>
 */
@Component
public class StoreConfigCache {

    /**
     * 占位缓存，key 设计：tenantId:storeId:configVersion。
     */
    private final Map<String, StoreConfig> cache = new ConcurrentHashMap<>();

    public StoreConfig get(Long tenantId, Long storeId, long configVersion) {
        // key 带版本号，避免并发更新后的旧缓存被复用
        return cache.get(buildKey(tenantId, storeId, configVersion));
    }

    public void put(StoreConfig config) {
        if (config == null) {
            return;
        }
        // 未来接入 Redis 时此处可改为写穿透逻辑
        cache.put(buildKey(config.getTenantId(), config.getStoreId(), config.getConfigVersion()), config);
    }

    /**
     * 使指定门店的缓存失效，防止后续读取到旧版本配置。
     */
    public void evictStore(Long tenantId, Long storeId) {
        String prefix = "store:config:" + tenantId + ":" + storeId + ":";
        cache.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private String buildKey(Long tenantId, Long storeId, Long configVersion) {
        return "store:config:" + tenantId + ":" + storeId + ":" + configVersion;
    }
}
