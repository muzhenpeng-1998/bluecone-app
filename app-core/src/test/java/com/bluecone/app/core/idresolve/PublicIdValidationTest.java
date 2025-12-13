package com.bluecone.app.core.idresolve;

import com.bluecone.app.core.idresolve.api.ResolveKey;
import com.bluecone.app.core.idresolve.api.ResolveResult;
import com.bluecone.app.core.idresolve.application.CachedPublicIdResolver;
import com.bluecone.app.core.idresolve.config.IdResolveProperties;
import com.bluecone.app.core.idresolve.spi.PublicIdFallbackLookup;
import com.bluecone.app.core.idresolve.spi.PublicIdL2Cache;
import com.bluecone.app.core.idresolve.spi.PublicIdMapRepository;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.publicid.api.DecodedPublicId;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 公共 ID 校验相关行为测试：非法格式/前缀不匹配时不得访问仓储。
 */
class PublicIdValidationTest {

    @Test
    void invalidFormatShouldNotCallRepository() {
        PublicIdMapRepository repo = mock(PublicIdMapRepository.class);
        PublicIdCodec codec = mock(PublicIdCodec.class);
        PublicIdL2Cache l2Cache = mock(PublicIdL2Cache.class);

        CachedPublicIdResolver resolver = new CachedPublicIdResolver(
                repo,
                l2Cache,
                codec,
                new IdResolveProperties(),
                List.<PublicIdFallbackLookup>of(),
                null,
                Clock.systemUTC()
        );

        String invalid = "invalid-format";
        org.mockito.Mockito.when(codec.decode(invalid))
                .thenThrow(new IllegalArgumentException("invalid format"));

        ResolveResult result = resolver.resolve(new ResolveKey(1L, ResourceType.STORE, invalid));

        assertThat(result.hit()).isFalse();
        assertThat(result.exists()).isFalse();
        assertThat(result.reason()).isEqualTo(ResolveResult.REASON_INVALID_FORMAT);
        verify(repo, never()).findInternalId(any(Long.class), any(String.class), any(String.class));
    }

    @Test
    void prefixMismatchShouldNotCallRepository() {
        PublicIdMapRepository repo = mock(PublicIdMapRepository.class);
        PublicIdCodec codec = mock(PublicIdCodec.class);

        CachedPublicIdResolver resolver = new CachedPublicIdResolver(
                repo,
                null,
                codec,
                new IdResolveProperties(),
                List.<PublicIdFallbackLookup>of(),
                null,
                Clock.systemUTC()
        );

        String publicId = "ord_01ABCDEF0123456789ABCDEFGH";
        org.mockito.Mockito.when(codec.decode(publicId))
                .thenReturn(new DecodedPublicId("ord", new Ulid128(1L, 2L)));

        ResolveResult result = resolver.resolve(new ResolveKey(1L, ResourceType.STORE, publicId));

        assertThat(result.hit()).isFalse();
        assertThat(result.exists()).isFalse();
        assertThat(result.reason()).isEqualTo(ResolveResult.REASON_PREFIX_MISMATCH);
        verify(repo, never()).findInternalId(any(Long.class), any(String.class), any(String.class));
    }
}

