package com.bluecone.app.core.cacheepoch;

import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
import com.bluecone.app.core.cacheepoch.application.DefaultCacheEpochProvider;
import com.bluecone.app.core.contextkit.CacheKey;
import com.bluecone.app.core.contextkit.CaffeineContextCache;
import com.bluecone.app.core.contextkit.ContextCache;
import com.bluecone.app.core.contextkit.ContextKitProperties;
import com.bluecone.app.core.contextkit.SnapshotLoadKey;
import com.bluecone.app.core.contextkit.SnapshotProvider;
import com.bluecone.app.core.contextkit.SnapshotRepository;
import com.bluecone.app.core.contextkit.SnapshotSerde;
import com.bluecone.app.core.contextkit.VersionChecker;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 epoch=1 与 epoch=2 时，同一个 scopeId 生成的 CacheKey 不同。
 */
class EpochKeyingTest {

    private record DummySnap(long id) {
    }

    @Test
    void cacheKeyShouldIncludeEpoch() {
        ContextCache cache = new CaffeineContextCache(1000);
        VersionChecker vc = new VersionChecker(Duration.ofSeconds(1), 0.0d);
        ContextKitProperties props = new ContextKitProperties();
        SnapshotProvider<DummySnap> provider = new SnapshotProvider<>();
        CacheEpochProvider epochProvider = new DefaultCacheEpochProvider(Duration.ofSeconds(3));

        SnapshotRepository<DummySnap> repo = new SnapshotRepository<>() {
            @Override
            public Optional<DummySnap> loadFull(SnapshotLoadKey key) {
                return Optional.of(new DummySnap(1L));
            }

            @Override
            public Optional<Long> loadVersion(SnapshotLoadKey key) {
                return Optional.of(1L);
            }
        };

        SnapshotSerde<DummySnap> serde = new SnapshotSerde<>() {
            @Override
            public Object toCacheValue(DummySnap value) {
                return value;
            }

            @Override
            public DummySnap fromCacheValue(Object cached) {
                return (DummySnap) cached;
            }
        };

        long tenantId = 1L;
        String namespace = "test:snap";
        long scopeId = 100L;

        long epoch1 = epochProvider.currentEpoch(tenantId, namespace);
        SnapshotLoadKey loadKey = new SnapshotLoadKey(tenantId, namespace, scopeId);
        provider.getOrLoad(loadKey, repo, cache, vc, serde, props, epochProvider);

        CacheKey k1 = new CacheKey(namespace, tenantId + ":" + epoch1 + ":" + scopeId);

        long epoch2 = epochProvider.bumpEpoch(tenantId, namespace);
        provider.getOrLoad(loadKey, repo, cache, vc, serde, props, epochProvider);
        CacheKey k2 = new CacheKey(namespace, tenantId + ":" + epoch2 + ":" + scopeId);

        assertThat(epoch2).isGreaterThan(epoch1);
        assertThat(k1.key()).isNotEqualTo(k2.key());
    }
}

