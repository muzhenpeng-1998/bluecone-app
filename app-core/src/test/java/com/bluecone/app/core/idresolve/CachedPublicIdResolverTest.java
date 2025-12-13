package com.bluecone.app.core.idresolve;

import com.bluecone.app.core.idresolve.api.ResolveKey;
import com.bluecone.app.core.idresolve.api.ResolveResult;
import com.bluecone.app.core.idresolve.application.CachedPublicIdResolver;
import com.bluecone.app.core.idresolve.config.IdResolveProperties;
import com.bluecone.app.core.idresolve.spi.PublicIdFallbackLookup;
import com.bluecone.app.core.idresolve.spi.PublicIdL2Cache;
import com.bluecone.app.core.idresolve.spi.PublicIdL2CacheResult;
import com.bluecone.app.core.idresolve.spi.PublicIdMapRepository;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.publicid.api.DecodedPublicId;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CachedPublicIdResolver 多级缓存行为测试。
 */
class CachedPublicIdResolverTest {

    private CachedPublicIdResolver newResolver(PublicIdMapRepository repo,
                                               PublicIdL2Cache l2Cache,
                                               IdResolveProperties properties) {
        PublicIdCodec codec = new StubPublicIdCodec();
        return new CachedPublicIdResolver(
                repo,
                l2Cache,
                codec,
                properties,
                List.<PublicIdFallbackLookup>of(),
                null,
                Clock.systemUTC()
        );
    }

    @Test
    void l1HitShouldBypassL2AndDb() {
        PublicIdMapRepository repo = mock(PublicIdMapRepository.class);
        PublicIdL2Cache l2 = mock(PublicIdL2Cache.class);
        IdResolveProperties props = new IdResolveProperties();

        CachedPublicIdResolver resolver = newResolver(repo, l2, props);

        Ulid128 id = new Ulid128(1L, 2L);
        when(l2.get(anyLong(), any(), any())).thenReturn(PublicIdL2CacheResult.miss());
        when(repo.findInternalId(1L, "STORE", "sto_1")).thenReturn(Optional.of(id));

        ResolveKey key = new ResolveKey(1L, ResourceType.STORE, "sto_1");

        ResolveResult first = resolver.resolve(key);
        assertThat(first.exists()).isTrue();
        assertThat(first.internalId()).isEqualTo(id);

        ResolveResult second = resolver.resolve(key);
        assertThat(second.exists()).isTrue();
        assertThat(second.internalId()).isEqualTo(id);

        verify(repo, times(1)).findInternalId(1L, "STORE", "sto_1");
        verify(l2, times(1)).get(1L, ResourceType.STORE, "sto_1");
    }

    @Test
    void l2HitShouldBackfillL1() {
        PublicIdMapRepository repo = mock(PublicIdMapRepository.class);
        PublicIdL2Cache l2 = mock(PublicIdL2Cache.class);
        IdResolveProperties props = new IdResolveProperties();

        CachedPublicIdResolver resolver = newResolver(repo, l2, props);

        Ulid128 id = new Ulid128(3L, 4L);
        when(l2.get(1L, ResourceType.STORE, "sto_2"))
                .thenReturn(PublicIdL2CacheResult.positiveHit(id));

        ResolveKey key = new ResolveKey(1L, ResourceType.STORE, "sto_2");

        ResolveResult first = resolver.resolve(key);
        assertThat(first.exists()).isTrue();
        assertThat(first.internalId()).isEqualTo(id);

        ResolveResult second = resolver.resolve(key);
        assertThat(second.exists()).isTrue();
        assertThat(second.internalId()).isEqualTo(id);

        verify(repo, never()).findInternalId(anyLong(), any(), any());
        verify(l2, times(1)).get(1L, ResourceType.STORE, "sto_2");
    }

    @Test
    void dbHitShouldWriteL1AndL2() {
        PublicIdMapRepository repo = mock(PublicIdMapRepository.class);
        PublicIdL2Cache l2 = mock(PublicIdL2Cache.class);
        IdResolveProperties props = new IdResolveProperties();

        CachedPublicIdResolver resolver = newResolver(repo, l2, props);

        Ulid128 id = new Ulid128(5L, 6L);
        when(l2.get(anyLong(), any(), any())).thenReturn(PublicIdL2CacheResult.miss());
        when(repo.findInternalId(1L, "STORE", "sto_3")).thenReturn(Optional.of(id));

        ResolveKey key = new ResolveKey(1L, ResourceType.STORE, "sto_3");

        ResolveResult result = resolver.resolve(key);
        assertThat(result.exists()).isTrue();
        assertThat(result.internalId()).isEqualTo(id);

        verify(repo, times(1)).findInternalId(1L, "STORE", "sto_3");
        verify(l2, times(1)).get(1L, ResourceType.STORE, "sto_3");
        verify(l2, times(1)).putPositive(eq(1L), eq(ResourceType.STORE), eq("sto_3"), eq(id), any(Duration.class));
    }

    @Test
    void dbMissShouldWriteNegativeCacheWithShortTtl() {
        PublicIdMapRepository repo = mock(PublicIdMapRepository.class);
        PublicIdL2Cache l2 = mock(PublicIdL2Cache.class);
        IdResolveProperties props = new IdResolveProperties();
        props.getCache().setNegativeTtl(Duration.ofSeconds(30));

        CachedPublicIdResolver resolver = newResolver(repo, l2, props);

        when(l2.get(anyLong(), any(), any())).thenReturn(PublicIdL2CacheResult.miss());
        when(repo.findInternalId(1L, "STORE", "sto_4")).thenReturn(Optional.empty());

        ResolveKey key = new ResolveKey(1L, ResourceType.STORE, "sto_4");

        ResolveResult result = resolver.resolve(key);
        assertThat(result.exists()).isFalse();

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(l2, times(1)).putNegative(eq(1L), eq(ResourceType.STORE), eq("sto_4"), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofSeconds(30));

        // 第二次调用应命中负缓存，不再访问 DB。
        ResolveResult second = resolver.resolve(key);
        assertThat(second.exists()).isFalse();
        verify(repo, times(1)).findInternalId(1L, "STORE", "sto_4");
    }

    /**
     * 仅用于测试的轻量 PublicIdCodec 实现。
     */
    static class StubPublicIdCodec implements PublicIdCodec {

        @Override
        public com.bluecone.app.id.publicid.api.PublicId encode(String type, Ulid128 id) {
            throw new UnsupportedOperationException("encode not used in tests");
        }

        @Override
        public com.bluecone.app.id.publicid.api.PublicId encode(String type, byte[] ulidBytes16) {
            throw new UnsupportedOperationException("encode not used in tests");
        }

        @Override
        public DecodedPublicId decode(String publicId) {
            if (publicId == null || publicId.isBlank()) {
                throw new IllegalArgumentException("empty publicId");
            }
            int idx = publicId.indexOf('_');
            if (idx <= 0) {
                throw new IllegalArgumentException("missing prefix");
            }
            String type = publicId.substring(0, idx);
            return new DecodedPublicId(type, new Ulid128(1L, 2L));
        }

        @Override
        public boolean isValid(String publicId) {
            try {
                decode(publicId);
                return true;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
    }
}

