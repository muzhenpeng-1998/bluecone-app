package com.bluecone.app.inventory.runtime;

import com.bluecone.app.core.contextkit.CacheKey;
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
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * InventoryPolicySnapshotProvider 版本不一致时触发 reload 行为测试。
 */
class InventoryPolicySnapshotProviderVersionReloadTest {

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
        InventoryPolicyRepository repo = Mockito.mock(InventoryPolicyRepository.class);

        Ulid128 internalId = new Ulid128(1L, 2L);
        InventoryPolicySnapshot snapshotV1 = new InventoryPolicySnapshot(
                1L,
                internalId,
                "sto_1",
                1L,
                true,
                "ON_ORDER",
                0,
                Instant.now(),
                Map.of()
        );
        InventoryPolicySnapshot snapshotV2 = new InventoryPolicySnapshot(
                1L,
                internalId,
                "sto_1",
                2L,
                true,
                "ON_PAID",
                0,
                Instant.now(),
                Map.of()
        );

        when(repo.loadFull(any(SnapshotLoadKey.class)))
                .thenReturn(Optional.of(snapshotV1))
                .thenReturn(Optional.of(snapshotV2));
        when(repo.loadVersion(any(SnapshotLoadKey.class)))
                .thenReturn(Optional.of(1L))
                .thenReturn(Optional.of(2L))
                .thenReturn(Optional.of(2L));

        ContextCache cache = new CaffeineContextCache(1000);
        VersionChecker versionChecker = new AlwaysCheckVersionChecker();
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

        Optional<InventoryPolicySnapshot> r1 = provider.getOrLoad(1L, internalId, "sto_1", 100L);
        Optional<InventoryPolicySnapshot> r2 = provider.getOrLoad(1L, internalId, "sto_1", 100L);

        assertThat(r1).isPresent();
        assertThat(r2).isPresent();
        assertThat(r1.get().deductMode()).isEqualTo("ON_ORDER");
        assertThat(r2.get().deductMode()).isEqualTo("ON_PAID");

        Mockito.verify(repo, Mockito.times(2)).loadFull(any(SnapshotLoadKey.class));
    }
}

