package com.bluecone.app.cacheinval;

import com.bluecone.app.cacheinval.transport.InProcessCacheInvalidationBus;
import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
import com.bluecone.app.core.cacheepoch.application.DefaultCacheEpochProvider;
import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;
import com.bluecone.app.core.cacheinval.api.InvalidationScope;
import com.bluecone.app.core.cacheinval.application.CacheInvalidationExecutor;
import com.bluecone.app.core.cacheinval.application.DefaultCacheInvalidationExecutor;
import com.bluecone.app.core.cacheinval.transport.CacheInvalidationBus;
import com.bluecone.app.core.contextkit.CacheNamespaces;
import com.bluecone.app.core.contextkit.CaffeineContextCache;
import com.bluecone.app.core.contextkit.ContextCache;
import com.bluecone.app.core.contextkit.ContextKitProperties;
import com.bluecone.app.core.contextkit.SnapshotLoadKey;
import com.bluecone.app.core.contextkit.SnapshotProvider;
import com.bluecone.app.core.contextkit.SnapshotRepository;
import com.bluecone.app.core.contextkit.SnapshotSerde;
import com.bluecone.app.core.contextkit.VersionChecker;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.store.runtime.api.StoreSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端验证：StoreSnapshot 在 DIRECT_KEYS 失效后能够读取到新快照，
 * 且包含 epoch keying 行为（通过 CacheEpochProvider 控制）。
 */
class StoreSnapshotInvalidationE2ETest {

    @Test
    void storeSnapshotShouldBeReloadedAfterInvalidation() {
        long tenantId = 1L;
        Ulid128 internalId = new Ulid128(1L, 2L);

        // epoch provider & cache
        CacheEpochProvider epochProvider = new DefaultCacheEpochProvider(Duration.ofSeconds(3));
        ContextCache cache = new CaffeineContextCache(1000);
        ContextKitProperties props = new ContextKitProperties();
        props.setL1Ttl(Duration.ofMinutes(5));
        props.setNegativeTtl(Duration.ofSeconds(30));
        VersionChecker versionChecker = new VersionChecker(Duration.ofSeconds(1), 1.0d);

        // in-memory store snapshot repository
        InMemoryStoreSnapshotRepo repo = new InMemoryStoreSnapshotRepo();

        SnapshotProvider<StoreSnapshot> provider = new SnapshotProvider<>();
        SnapshotSerde<StoreSnapshot> serde = new SnapshotSerde<>() {
            @Override
            public Object toCacheValue(StoreSnapshot value) {
                return value;
            }

            @Override
            public StoreSnapshot fromCacheValue(Object cached) {
                return (StoreSnapshot) cached;
            }
        };

        SnapshotLoadKey loadKey = new SnapshotLoadKey(tenantId, CacheNamespaces.STORE_SNAPSHOT, internalId);

        // 1) 首次加载，写入缓存（版本 v1）
        repo.setSnapshot(buildSnapshot(tenantId, internalId, 1L));
        StoreSnapshot first = provider.getOrLoad(loadKey, repo, cache, versionChecker, serde, props, epochProvider);
        assertThat(first).isNotNull();
        assertThat(first.configVersion()).isEqualTo(1L);

        long epoch1 = epochProvider.currentEpoch(tenantId, CacheNamespaces.STORE_SNAPSHOT);

        // 2) 更新底层快照为 v2
        repo.setSnapshot(buildSnapshot(tenantId, internalId, 2L));

        // 3) 构造 DIRECT_KEYS 失效事件（不带 epoch）
        String semanticKey = tenantId + ":" + internalId.toString();
        CacheInvalidationEvent evt = new CacheInvalidationEvent(
                "e-store-1",
                tenantId,
                InvalidationScope.STORE,
                CacheNamespaces.STORE_SNAPSHOT,
                java.util.List.of(semanticKey),
                2L,
                Instant.now()
        );

        CacheInvalidationExecutor executor = new DefaultCacheInvalidationExecutor(cache, epochProvider);
        CacheInvalidationBus bus = new InProcessCacheInvalidationBus();
        CacheInvalidationListener listener = new CacheInvalidationListener(
                executor,
                bus,
                new com.bluecone.app.config.CacheInvalidationProperties(),
                com.bluecone.app.core.cacheinval.observability.api.CacheInvalidationLogWriter.NOOP,
                "test-instance",
                "INPROCESS",
                epochProvider
        );

        // 4) 广播事件，触发本地失效
        bus.broadcast(evt);

        long epoch2 = epochProvider.currentEpoch(tenantId, CacheNamespaces.STORE_SNAPSHOT);
        assertThat(epoch2).isEqualTo(epoch1); // DIRECT_KEYS 模式不应 bump epoch

        // 5) 再次加载，应读取到新版本 v2
        StoreSnapshot second = provider.getOrLoad(loadKey, repo, cache, versionChecker, serde, props, epochProvider);
        assertThat(second).isNotNull();
        assertThat(second.configVersion()).isEqualTo(2L);
    }

    private StoreSnapshot buildSnapshot(long tenantId, Ulid128 internalId, long configVersion) {
        return new StoreSnapshot(
                tenantId,
                internalId,
                "sto-test",
                "Test Store",
                1,
                true,
                null,
                configVersion,
                Instant.now(),
                Map.of()
        );
    }

    private static class InMemoryStoreSnapshotRepo implements SnapshotRepository<StoreSnapshot> {

        private StoreSnapshot snapshot;

        void setSnapshot(StoreSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public Optional<StoreSnapshot> loadFull(SnapshotLoadKey key) {
            return Optional.ofNullable(snapshot);
        }

        @Override
        public Optional<Long> loadVersion(SnapshotLoadKey key) {
            return Optional.ofNullable(snapshot).map(StoreSnapshot::configVersion);
        }
    }
}

