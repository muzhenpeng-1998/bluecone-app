package com.bluecone.app.application.middleware;

import com.bluecone.app.config.StoreContextProperties;
import com.bluecone.app.core.idresolve.api.PublicIdResolver;
import com.bluecone.app.core.idresolve.api.ResolveKey;
import com.bluecone.app.core.idresolve.api.ResolveResult;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.store.runtime.application.StoreSnapshotProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * StoreContextResolver 校验行为测试：prefix 不匹配时不调用 snapshotProvider。
 */
class StoreContextResolverValidationTest {

    @Test
    void prefixMismatchShouldNotLoadSnapshot() {
        PublicIdResolver publicIdResolver = mock(PublicIdResolver.class);
        StoreSnapshotProvider provider = mock(StoreSnapshotProvider.class);
        StoreContextProperties props = new StoreContextProperties();
        props.setIncludePaths(java.util.List.of("/api/mini/**"));
        props.setExcludePaths(java.util.List.of());

        StoreContextResolver resolver = new StoreContextResolver(publicIdResolver, provider, props);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/mini/order");
        when(request.getHeader("X-Tenant-Id")).thenReturn("1");
        when(request.getHeader("X-Store-Id")).thenReturn("ord_abc"); // prefix 与 STORE 不匹配

        ResolveResult invalid = new ResolveResult(false, false, null, "ord_abc", ResolveResult.REASON_PREFIX_MISMATCH);
        when(publicIdResolver.resolve(any(ResolveKey.class))).thenReturn(invalid);

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(com.bluecone.app.core.idresolve.api.PublicIdInvalidException.class);

        verify(provider, never()).getOrLoad(anyLong(), any(), any());
    }
}

