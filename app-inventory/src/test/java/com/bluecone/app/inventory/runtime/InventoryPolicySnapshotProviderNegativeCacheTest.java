package com.bluecone.app.inventory.runtime;

import com.bluecone.app.core.contextkit.CaffeineContextCache;
import com.bluecone.app.core.contextkit.ContextCache;
import com.bluecone.app.core.contextkit.ContextKitProperties;
import com.bluecone.app.core.contextkit.SnapshotLoadKey;
import com.bluecone.app.core.contextkit.VersionChecker;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.inventory.runtime.api.InventoryPolicySnapshot;
import com.bluecone.app.inventory.runtime.application.InventoryPolicySnapshotProvider;
import com.bluecone.app.inventory.runtime.spi.InventoryPolicyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * InventoryPolicySnapshotProvider 负缓存行为测试：notfound 时写负缓存，第二次请求不打 DB。
 */
class InventoryPolicySnapshotProviderNegativeCacheTest {

    @Test
    void notFoundShouldUseNegativeCache() {
        InventoryPolicyRepository repo = Mockito.mock(InventoryPolicyRepository.class);
        when(repo.loadFull(any(SnapshotLoadKey.class))).thenReturn(Optional.empty());

        ContextCache cache = new CaffeineContextCache(1000);
        VersionChecker versionChecker = new VersionChecker(Duration.ofSeconds(2), 1.0d);
        ContextKitProperties kitProps = new ContextKitProperties();
        kitProps.setL1Ttl(Duration.ofMinutes(5));
        kitProps.setNegativeTtl(Duration.ofSeconds(30));

        ObjectMapper objectMapper = new ObjectMapper();

        InventoryPolicySnapshotProvider provider = new InventoryPolicySnapshotProvider(
                repo,
                cache,
                versionChecker,
                kitProps,
                objectMapper);

        Ulid128 internalId = new Ulid128(1L, 2L);
        Optional<InventoryPolicySnapshot> r1 = provider.getOrLoad(1L, internalId, "sto_1", 100L);
        Optional<InventoryPolicySnapshot> r2 = provider.getOrLoad(1L, internalId, "sto_1", 100L);

        assertThat(r1).isEmpty();
        assertThat(r2).isEmpty();
        Mockito.verify(repo, times(1)).loadFull(any(SnapshotLoadKey.class));
    }
}
