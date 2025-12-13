package com.bluecone.app.core.user.runtime;

import com.bluecone.app.core.contextkit.CacheKey;
import com.bluecone.app.core.contextkit.CaffeineContextCache;
import com.bluecone.app.core.contextkit.ContextCache;
import com.bluecone.app.core.contextkit.ContextKitProperties;
import com.bluecone.app.core.contextkit.SnapshotLoadKey;
import com.bluecone.app.core.contextkit.SnapshotProvider;
import com.bluecone.app.core.contextkit.SnapshotSerde;
import com.bluecone.app.core.contextkit.VersionChecker;
import com.bluecone.app.core.user.runtime.api.UserSnapshot;
import com.bluecone.app.core.user.runtime.spi.UserSnapshotRepository;
import com.bluecone.app.id.core.Ulid128;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class UserSnapshotProviderVersionReloadTest {

    static class AlwaysCheckVersionChecker extends VersionChecker {
        AlwaysCheckVersionChecker() {
            super(Duration.ofSeconds(1), 1.0d);
        }

        @Override
        public boolean shouldCheck(CacheKey key) {
            return true;
        }

        @Override
        public void markChecked(CacheKey key) {
        }
    }

    @Test
    void versionMismatchShouldReloadSnapshot() {
        UserSnapshotRepository repo = Mockito.mock(UserSnapshotRepository.class);
        ContextCache cache = new CaffeineContextCache(1000);
        VersionChecker versionChecker = new AlwaysCheckVersionChecker();
        ContextKitProperties kitProps = new ContextKitProperties();
        kitProps.setL1Ttl(Duration.ofMinutes(5));
        kitProps.setNegativeTtl(Duration.ofSeconds(30));

        SnapshotProvider<UserSnapshot> provider = new SnapshotProvider<>();
        ObjectMapper objectMapper = new ObjectMapper();
        SnapshotSerde<UserSnapshot> serde = new SnapshotSerde<>() {
            @Override
            public Object toCacheValue(UserSnapshot value) {
                return value;
            }

            @Override
            public UserSnapshot fromCacheValue(Object cached) {
                if (cached instanceof UserSnapshot s) {
                    return s;
                }
                return objectMapper.convertValue(cached, UserSnapshot.class);
            }
        };

        Ulid128 internalId = new Ulid128(1L, 2L);
        UserSnapshot snapV1 = new UserSnapshot(
                1L,
                internalId,
                1,
                true,
                "L1",
                1L,
                Instant.now(),
                Map.of()
        );
        UserSnapshot snapV2 = new UserSnapshot(
                1L,
                internalId,
                1,
                true,
                "L2",
                2L,
                Instant.now(),
                Map.of()
        );

        when(repo.loadFull(any(SnapshotLoadKey.class)))
                .thenReturn(Optional.of(snapV1))
                .thenReturn(Optional.of(snapV2));
        when(repo.loadVersion(any(SnapshotLoadKey.class)))
                .thenReturn(Optional.of(1L))
                .thenReturn(Optional.of(2L))
                .thenReturn(Optional.of(2L));

        SnapshotLoadKey key = new SnapshotLoadKey(1L, "user:snap", internalId);

        UserSnapshot r1 = provider.getOrLoad(key, repo, cache, versionChecker, serde, kitProps);
        UserSnapshot r2 = provider.getOrLoad(key, repo, cache, versionChecker, serde, kitProps);

        assertThat(r1.memberLevel()).isEqualTo("L1");
        assertThat(r2.memberLevel()).isEqualTo("L2");
        Mockito.verify(repo, Mockito.times(2)).loadFull(any(SnapshotLoadKey.class));
    }
}

