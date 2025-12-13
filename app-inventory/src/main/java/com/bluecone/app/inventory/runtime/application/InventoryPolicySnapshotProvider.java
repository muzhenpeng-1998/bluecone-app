package com.bluecone.app.inventory.runtime.application;

import com.bluecone.app.core.contextkit.ContextCache;
import com.bluecone.app.core.contextkit.ContextKitProperties;
import com.bluecone.app.core.contextkit.SnapshotLoadKey;
import com.bluecone.app.core.contextkit.SnapshotProvider;
import com.bluecone.app.core.contextkit.SnapshotSerde;
import com.bluecone.app.core.contextkit.VersionChecker;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.inventory.runtime.api.InventoryPolicySnapshot;
import com.bluecone.app.inventory.runtime.api.InventoryScope;
import com.bluecone.app.inventory.runtime.spi.InventoryPolicyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

/**
 * 库存策略快照 Provider，复用 ContextMiddlewareKit 通用能力：
 * - L1/L2 多级缓存
 * - 版本校验窗口 + 采样
 * - 负缓存防扫库
 */
public class InventoryPolicySnapshotProvider {

    private final InventoryPolicyRepository repository;
    private final ContextCache cache;
    private final VersionChecker versionChecker;
    private final ContextKitProperties kitProperties;
    private final SnapshotProvider<InventoryPolicySnapshot> delegate;
    private final SnapshotSerde<InventoryPolicySnapshot> serde;

    public InventoryPolicySnapshotProvider(InventoryPolicyRepository repository,
                                           ContextCache cache,
                                           VersionChecker versionChecker,
                                           ContextKitProperties kitProperties,
                                           ObjectMapper objectMapper) {
        this.repository = repository;
        this.cache = cache;
        this.versionChecker = versionChecker;
        this.kitProperties = kitProperties;
        this.delegate = new SnapshotProvider<>();
        this.serde = new InventoryPolicySnapshotSerde(objectMapper);
    }

    /**
     * 获取或加载库存策略快照。
     */
    public Optional<InventoryPolicySnapshot> getOrLoad(long tenantId,
                                                       Ulid128 storeInternalId,
                                                       String storePublicId,
                                                       Long storeNumericId) {
        InventoryScope scope = new InventoryScope(storeInternalId, storePublicId, storeNumericId);
        SnapshotLoadKey loadKey = new SnapshotLoadKey(tenantId, "inventory:policy", scope);
        InventoryPolicySnapshot snapshot = delegate.getOrLoad(
                loadKey,
                repository,
                cache,
                versionChecker,
                serde,
                kitProperties
        );
        return Optional.ofNullable(snapshot);
    }

    /**
     * 库存策略快照序列化适配器：L1 直接缓存对象，L2 通过 Jackson 反序列化。
     */
    private static class InventoryPolicySnapshotSerde implements SnapshotSerde<InventoryPolicySnapshot> {

        private final ObjectMapper objectMapper;

        private InventoryPolicySnapshotSerde(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Object toCacheValue(InventoryPolicySnapshot value) {
            return value;
        }

        @Override
        public InventoryPolicySnapshot fromCacheValue(Object cached) {
            if (cached == null) {
                return null;
            }
            if (cached instanceof InventoryPolicySnapshot snapshot) {
                return snapshot;
            }
            return objectMapper.convertValue(cached, InventoryPolicySnapshot.class);
        }
    }
}
