package com.bluecone.app.core.contextkit;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SnapshotProvider 行为测试：缓存命中、版本失效重载、负缓存命中。
 */
class SnapshotProviderTest {

    private record DummySnap(long id, long version) {
    }

    @Test
    void cacheHitShouldBypassRepository() {
        InMemoryRepo repo = new InMemoryRepo();
        repo.snapshot = new DummySnap(1L, 1L);
        ContextCache cache = new CaffeineContextCache(1000);
        VersionChecker vc = new VersionChecker(Duration.ofSeconds(2), 0.0d);
        ContextKitProperties props = new ContextKitProperties();
        SnapshotProvider<DummySnap> provider = new SnapshotProvider<>();

        SnapshotLoadKey key = new SnapshotLoadKey(1L, "test", 1L);

        DummySnap s1 = provider.getOrLoad(key, repo, cache, vc, new DummySerde(), props);
        DummySnap s2 = provider.getOrLoad(key, repo, cache, vc, new DummySerde(), props);

        assertThat(s1).isNotNull();
        assertThat(s2).isNotNull();
        assertThat(repo.loadFullCalls.get()).isEqualTo(1);
    }

    @Test
    void versionChangedShouldReload() {
        InMemoryRepo repo = new InMemoryRepo();
        repo.snapshot = new DummySnap(1L, 1L);
        ContextCache cache = new CaffeineContextCache(1000);
        VersionChecker vc = new VersionChecker(Duration.ZERO, 1.0d);
        ContextKitProperties props = new ContextKitProperties();
        SnapshotProvider<DummySnap> provider = new SnapshotProvider<>();

        SnapshotLoadKey key = new SnapshotLoadKey(1L, "test", 1L);

        DummySnap first = provider.getOrLoad(key, repo, cache, vc, new DummySerde(), props);
        assertThat(first.version()).isEqualTo(1L);

        repo.snapshot = new DummySnap(1L, 2L);

        DummySnap second = provider.getOrLoad(key, repo, cache, vc, new DummySerde(), props);
        assertThat(second.version()).isEqualTo(2L);
        assertThat(repo.loadFullCalls.get()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void negativeCacheShouldAvoidSecondDbHit() {
        InMemoryRepo repo = new InMemoryRepo();
        repo.snapshot = null;
        ContextCache cache = new CaffeineContextCache(1000);
        VersionChecker vc = new VersionChecker(Duration.ofSeconds(2), 1.0d);
        ContextKitProperties props = new ContextKitProperties();
        SnapshotProvider<DummySnap> provider = new SnapshotProvider<>();

        SnapshotLoadKey key = new SnapshotLoadKey(1L, "test", 1L);

        DummySnap first = provider.getOrLoad(key, repo, cache, vc, new DummySerde(), props);
        DummySnap second = provider.getOrLoad(key, repo, cache, vc, new DummySerde(), props);

        assertThat(first).isNull();
        assertThat(second).isNull();
        assertThat(repo.loadFullCalls.get()).isEqualTo(1);
    }

    private static class InMemoryRepo implements SnapshotRepository<DummySnap> {

        private final AtomicInteger loadFullCalls = new AtomicInteger();
        private DummySnap snapshot;

        @Override
        public Optional<DummySnap> loadFull(SnapshotLoadKey key) {
            loadFullCalls.incrementAndGet();
            return Optional.ofNullable(snapshot);
        }

        @Override
        public Optional<Long> loadVersion(SnapshotLoadKey key) {
            return Optional.ofNullable(snapshot).map(DummySnap::version);
        }
    }

    private static class DummySerde implements SnapshotSerde<DummySnap> {

        @Override
        public Object toCacheValue(DummySnap value) {
            return value;
        }

        @Override
        public DummySnap fromCacheValue(Object cached) {
            return (DummySnap) cached;
        }
    }
}

