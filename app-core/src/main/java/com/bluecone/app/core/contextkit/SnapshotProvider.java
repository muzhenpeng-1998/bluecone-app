package com.bluecone.app.core.contextkit;

import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
import java.util.Optional;

/**
 * 通用快照 Provider：封装缓存、多级版本校验与负缓存。
 */
public class SnapshotProvider<T> {

    public T getOrLoad(SnapshotLoadKey loadKey,
                       SnapshotRepository<T> repository,
                       ContextCache cache,
                       VersionChecker versionChecker,
                       SnapshotSerde<T> serde,
                       ContextKitProperties props) {
        return getOrLoad(loadKey, repository, cache, versionChecker, serde, props, null);
    }

    /**
     * 带 namespace epoch 的版本，允许根据租户+namespace 生成带 epoch 的缓存键。
     *
     * <p>当 {@code epochProvider} 为 null 时，退化为旧行为：
     * key = {@code tenantId:scopeId}。</p>
     */
    public T getOrLoad(SnapshotLoadKey loadKey,
                       SnapshotRepository<T> repository,
                       ContextCache cache,
                       VersionChecker versionChecker,
                       SnapshotSerde<T> serde,
                       ContextKitProperties props,
                       CacheEpochProvider epochProvider) {
        String namespace = loadKey.scopeType();
        String keySuffix;
        if (epochProvider != null) {
            long epoch = epochProvider.currentEpoch(loadKey.tenantId(), namespace);
            keySuffix = loadKey.tenantId() + ":" + epoch + ":" + String.valueOf(loadKey.scopeId());
        } else {
            keySuffix = loadKey.tenantId() + ":" + String.valueOf(loadKey.scopeId());
        }
        CacheKey cacheKey = new CacheKey(
                namespace,
                keySuffix
        );

        // 1) 读缓存
        Optional<CacheValue> cachedOpt = cache.get(cacheKey);
        if (cachedOpt.isPresent()) {
            CacheValue cached = cachedOpt.get();
            if (cached instanceof NegativeValue) {
                return null;
            }
            if (cached instanceof HitValue<?> hit) {
                T value = serde.fromCacheValue(hit.value());
                // 版本校验（基于 window + 采样）
                if (versionChecker.shouldCheck(cacheKey)) {
                    Optional<Long> dbVersion = repository.loadVersion(loadKey);
                    if (dbVersion.isPresent() && dbVersion.get() != hit.version()) {
                        versionChecker.markChecked(cacheKey);
                        return reloadAndFill(loadKey, repository, cache, serde, props, cacheKey);
                    }
                    versionChecker.markChecked(cacheKey);
                }
                return value;
            }
        }

        // 2) 缓存 miss，重新加载
        return reloadAndFill(loadKey, repository, cache, serde, props, cacheKey);
    }

    private T reloadAndFill(SnapshotLoadKey loadKey,
                            SnapshotRepository<T> repository,
                            ContextCache cache,
                            SnapshotSerde<T> serde,
                            ContextKitProperties props,
                            CacheKey cacheKey) {
        Optional<T> loaded = repository.loadFull(loadKey);
        if (loaded.isEmpty()) {
            cache.put(cacheKey, new NegativeValue("NOT_FOUND"), props.getNegativeTtl());
            return null;
        }
        T value = loaded.get();
        long version = repository.loadVersion(loadKey).orElse(0L);
        CacheValue hit = new HitValue<>(serde.toCacheValue(value), version);
        cache.put(cacheKey, hit, props.getL1Ttl());
        return value;
    }
}
