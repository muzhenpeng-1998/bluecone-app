package com.bluecone.app.product.runtime.application;

import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
import com.bluecone.app.core.contextkit.ContextCache;
import com.bluecone.app.core.contextkit.ContextKitProperties;
import com.bluecone.app.core.contextkit.SnapshotLoadKey;
import com.bluecone.app.core.contextkit.SnapshotProvider;
import com.bluecone.app.core.contextkit.SnapshotSerde;
import com.bluecone.app.core.contextkit.VersionChecker;
import com.bluecone.app.core.contextkit.CacheNamespaces;
import com.bluecone.app.product.runtime.api.ProductSnapshot;
import com.bluecone.app.product.runtime.spi.ProductSnapshotRepository;
import com.bluecone.app.id.core.Ulid128;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;

/**
 * 商品快照 Provider，复用 ContextMiddlewareKit 通用能力：
 * - L1/L2 多级缓存
 * - 版本校验窗口 + 采样
 * - 负缓存防扫库
 */
public class ProductSnapshotProvider {

    private final ProductSnapshotRepository repository;
    private final ContextCache cache;
    private final VersionChecker versionChecker;
    private final ContextKitProperties kitProperties;
    private final SnapshotProvider<ProductSnapshot> delegate;
    private final SnapshotSerde<ProductSnapshot> serde;
    private final CacheEpochProvider epochProvider;

    public ProductSnapshotProvider(ProductSnapshotRepository repository,
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
        this.serde = new ProductSnapshotSerde(objectMapper);
        this.epochProvider = epochProvider;
    }

    /**
     * 获取或加载商品快照。
     */
    public Optional<ProductSnapshot> getOrLoad(long tenantId, Ulid128 productId, String productPublicId) {
        SnapshotLoadKey loadKey = new SnapshotLoadKey(tenantId, CacheNamespaces.PRODUCT_SNAPSHOT, productId);
        ProductSnapshot snapshot = delegate.getOrLoad(
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

    private static class ProductSnapshotSerde implements SnapshotSerde<ProductSnapshot> {

        private final ObjectMapper objectMapper;

        private ProductSnapshotSerde(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Object toCacheValue(ProductSnapshot value) {
            return value;
        }

        @Override
        public ProductSnapshot fromCacheValue(Object cached) {
            if (cached == null) {
                return null;
            }
            if (cached instanceof ProductSnapshot snapshot) {
                return snapshot;
            }
            return objectMapper.convertValue(cached, ProductSnapshot.class);
        }
    }
}

