package com.bluecone.app.store.runtime;

import com.bluecone.app.core.contextkit.*;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.store.runtime.api.StoreSnapshot;
import com.bluecone.app.store.runtime.application.StoreSnapshotProvider;
import com.bluecone.app.store.runtime.spi.StoreSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * StoreSnapshotProvider 负缓存行为测试：notfound 时写负缓存，第二次请求不打 DB。
 */
class StoreSnapshotProviderNegativeCacheTest {

    @Test
    void notFoundShouldUseNegativeCache() {
        StoreSnapshotRepository repo = Mockito.mock(StoreSnapshotRepository.class);
        when(repo.loadSnapshot(anyLong(), any(Ulid128.class))).thenReturn(Optional.empty());

        ContextCache cache = new CaffeineContextCache(1000);
        VersionChecker versionChecker = new VersionChecker(Duration.ofSeconds(2), 1.0d);
        ContextKitProperties kitProps = new ContextKitProperties();
        kitProps.setL1Ttl(Duration.ofMinutes(5));
        kitProps.setNegativeTtl(Duration.ofSeconds(30));

        ObjectMapper objectMapper = new ObjectMapper();

        StoreSnapshotProvider provider = new StoreSnapshotProvider(
                repo,
                cache,
                versionChecker,
                kitProps,
                objectMapper);

        Ulid128 internalId = new Ulid128(1L, 2L);
        Optional<StoreSnapshot> r1 = provider.getOrLoad(1L, internalId, "sto_1");
        Optional<StoreSnapshot> r2 = provider.getOrLoad(1L, internalId, "sto_1");

        assertThat(r1).isEmpty();
        assertThat(r2).isEmpty();
        Mockito.verify(repo, times(1)).loadSnapshot(anyLong(), any(Ulid128.class));
    }
}
