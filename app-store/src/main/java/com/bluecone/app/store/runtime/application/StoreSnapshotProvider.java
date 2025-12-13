package com.bluecone.app.store.runtime.application;

import com.bluecone.app.core.contextkit.*;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.store.runtime.api.StoreSnapshot;
import com.bluecone.app.store.runtime.spi.StoreSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

/**
 * 门店快照 Provider，委托 ContextMiddlewareKit 的 SnapshotProvider 实现：
 * - L1/L2 多级缓存
 * - 版本校验窗口 + 采样
 * - 负缓存防扫库
 */
public class StoreSnapshotProvider {

    private final StoreSnapshotRepository repository;
    private final ContextCache cache;
    private final VersionChecker versionChecker;
    private final ContextKitProperties kitProperties;
    private final SnapshotProvider<StoreSnapshot> delegate;
    private final SnapshotSerde<StoreSnapshot> serde;

    public StoreSnapshotProvider(StoreSnapshotRepository repository,
                                 ContextCache cache,
                                 VersionChecker versionChecker,
                                 ContextKitProperties kitProperties,
                                 ObjectMapper objectMapper) {
        this.repository = repository;
        this.cache = cache;
        this.versionChecker = versionChecker;
        this.kitProperties = kitProperties;
        this.delegate = new SnapshotProvider<>();
        this.serde = new StoreSnapshotSerde(objectMapper);
    }

    /**
     * 获取或加载门店快照，内部通过 ContextMiddlewareKit 完成缓存与版本校验。
     */
    public Optional<StoreSnapshot> getOrLoad(long tenantId, Ulid128 storeInternalId, String storePublicId) {
        SnapshotLoadKey loadKey = new SnapshotLoadKey(tenantId, "store:snap", storeInternalId);
        StoreSnapshot snapshot = delegate.getOrLoad(
                loadKey,
                new StoreSnapshotRepoAdapter(repository),
                cache,
                versionChecker,
                serde,
                kitProperties
        );
        return Optional.ofNullable(snapshot);
    }

    /**
     * StoreSnapshotRepository 适配为通用 SnapshotRepository。
     */
    private static class StoreSnapshotRepoAdapter implements SnapshotRepository<StoreSnapshot> {

        private final StoreSnapshotRepository delegate;

        private StoreSnapshotRepoAdapter(StoreSnapshotRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public Optional<StoreSnapshot> loadFull(SnapshotLoadKey key) {
            return delegate.loadSnapshot(key.tenantId(), (Ulid128) key.scopeId());
        }

        @Override
        public Optional<Long> loadVersion(SnapshotLoadKey key) {
            return delegate.loadConfigVersion(key.tenantId(), (Ulid128) key.scopeId());
        }
    }

    /**
     * StoreSnapshot 专用序列化适配器：L1 直接缓存对象，L2 反序列化后通过 Jackson 还原。
     */
    private static class StoreSnapshotSerde implements SnapshotSerde<StoreSnapshot> {

        private final ObjectMapper objectMapper;

        private StoreSnapshotSerde(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Object toCacheValue(StoreSnapshot value) {
            return value;
        }

        @Override
        public StoreSnapshot fromCacheValue(Object cached) {
            if (cached == null) {
                return null;
            }
            if (cached instanceof StoreSnapshot snapshot) {
                return snapshot;
            }
            return objectMapper.convertValue(cached, StoreSnapshot.class);
        }
    }
}

