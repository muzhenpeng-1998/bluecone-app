package com.bluecone.app.product.runtime.application;

import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
import com.bluecone.app.core.contextkit.ContextCache;
import com.bluecone.app.core.contextkit.ContextKitProperties;
import com.bluecone.app.core.contextkit.SnapshotLoadKey;
import com.bluecone.app.core.contextkit.SnapshotProvider;
import com.bluecone.app.core.contextkit.SnapshotSerde;
import com.bluecone.app.core.contextkit.VersionChecker;
import com.bluecone.app.core.contextkit.CacheNamespaces;
import com.bluecone.app.product.runtime.api.SkuSnapshot;
import com.bluecone.app.product.runtime.spi.SkuSnapshotRepository;
import com.bluecone.app.id.core.Ulid128;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;

/**
 * SKU 快照 Provider，复用 ContextMiddlewareKit 通用能力：
 * - L1/L2 多级缓存
 * - 版本校验窗口 + 采样
 * - 负缓存防扫库
 */
public class SkuSnapshotProvider {

    private final SkuSnapshotRepository repository;
    private final ContextCache cache;
    private final VersionChecker versionChecker;
    private final ContextKitProperties kitProperties;
    private final SnapshotProvider<SkuSnapshot> delegate;
    private final SnapshotSerde<SkuSnapshot> serde;
    private final CacheEpochProvider epochProvider;

    public SkuSnapshotProvider(SkuSnapshotRepository repository,
                               ContextCache cache,
                               VersionChecker versionChecker,
                               ContextKitProperties kitProperties,
                               ObjectMapper objectMapper,
                               CacheEpochProvider epochProvider) {
        this.repository = repository;
        this.cache = cache;
        this.versionChecker = versionChecker;
        this.kitProperties = kitProperties;
        this.delegate = new SnapshotProvider<>();
        this.serde = new SkuSnapshotSerde(objectMapper);
        this.epochProvider = epochProvider;
    }

    /**
     * 获取或加载 SKU 快照。
     */
    public Optional<SkuSnapshot> getOrLoad(long tenantId, Ulid128 skuId, String skuPublicId) {
        SnapshotLoadKey loadKey = new SnapshotLoadKey(tenantId, CacheNamespaces.SKU_SNAPSHOT, skuId);
        SkuSnapshot snapshot = delegate.getOrLoad(
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

    private static class SkuSnapshotSerde implements SnapshotSerde<SkuSnapshot> {

        private final ObjectMapper objectMapper;

        private SkuSnapshotSerde(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Object toCacheValue(SkuSnapshot value) {
            return value;
        }

        @Override
        public SkuSnapshot fromCacheValue(Object cached) {
            if (cached == null) {
                return null;
            }
            if (cached instanceof SkuSnapshot snapshot) {
                return snapshot;
            }
            return objectMapper.convertValue(cached, SkuSnapshot.class);
        }
    }
}

