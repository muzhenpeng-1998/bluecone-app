package com.bluecone.app.store.infrastructure.cache;

import com.bluecone.app.store.api.dto.StoreOrderSnapshot;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 门店上下文快照缓存（可选），用于缓存 StoreOrderSnapshot，减少重复装配。
 * <p>高并发：key 携带 configVersion，避免旧配置覆盖。</p>
 * <p>高稳定：可与 StoreConfigCache 一起做多级缓存，当前为占位实现。</p>
 */
@Component
public class StoreContextCache {

    private final Map<String, StoreOrderSnapshot> cache = new ConcurrentHashMap<>();

    public StoreOrderSnapshot get(Long tenantId, Long storeId, String channelType, long configVersion) {
        // channelType + version 作为维度，确保不同渠道的快照隔离
        return cache.get(buildKey(tenantId, storeId, channelType, configVersion));
    }

    public void put(StoreOrderSnapshot snapshot, Long tenantId, Long storeId, String channelType) {
        if (snapshot == null || snapshot.getConfigVersion() == null) {
            return;
        }
        // 未来可在此做 TTL 控制或接入多级缓存
        cache.put(buildKey(tenantId, storeId, channelType, snapshot.getConfigVersion()), snapshot);
    }

    private String buildKey(Long tenantId, Long storeId, String channelType, Long configVersion) {
        return "store:snapshot:" + tenantId + ":" + storeId + ":" + channelType + ":" + configVersion;
    }
}
