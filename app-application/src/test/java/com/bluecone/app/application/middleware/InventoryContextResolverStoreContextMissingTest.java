package com.bluecone.app.application.middleware;

import com.bluecone.app.config.InventoryContextProperties;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.ApiContextHolder;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.inventory.domain.error.InventoryErrorCode;
import com.bluecone.app.inventory.runtime.api.InventoryPolicySnapshot;
import com.bluecone.app.inventory.runtime.application.InventoryPolicySnapshotProvider;
import com.bluecone.app.core.store.StoreContext;
import com.bluecone.app.core.store.StoreSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class InventoryContextResolverStoreContextMissingTest {

    @AfterEach
    void cleanup() {
        ApiContextHolder.clear();
    }

    @Test
    void storeContextMissingShouldThrowBadRequest() {
        InventoryPolicySnapshotProvider provider = mock(InventoryPolicySnapshotProvider.class);
        InventoryContextProperties props = new InventoryContextProperties();
        props.setEnabled(true);
        props.setIncludePaths(java.util.List.of("/api/mini/**"));
        props.setExcludePaths(java.util.List.of());

        InventoryContextResolver resolver = new InventoryContextResolver(provider, props);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/mini/order/create");

        ApiContext ctx = ApiContext.builder()
                .traceId("trace-1")
                .requestTime(LocalDateTime.now())
                .request(request)
                .contract(null)
                .apiEndpoint(null)
                .apiVersion(null)
                .pathVariables(Collections.emptyMap())
                .queryParams(new java.util.HashMap<>())
                .build();
        ctx.setTenantId("1");
        ApiContextHolder.set(ctx);

        assertThatThrownBy(() -> resolver.resolve(ctx))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("门店上下文缺失")
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    org.assertj.core.api.Assertions.assertThat(biz.getCode())
                            .isEqualTo(CommonErrorCode.BAD_REQUEST.getCode());
                });

        verifyNoInteractions(provider);
    }

    @Test
    void policyNotFoundShouldThrowInventoryNotFound() {
        InventoryPolicySnapshotProvider provider = mock(InventoryPolicySnapshotProvider.class);
        InventoryContextProperties props = new InventoryContextProperties();
        props.setEnabled(true);
        props.setIncludePaths(java.util.List.of("/api/mini/**"));
        props.setExcludePaths(java.util.List.of());

        InventoryContextResolver resolver = new InventoryContextResolver(provider, props);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/mini/order/create");

        ApiContext ctx = ApiContext.builder()
                .traceId("trace-2")
                .requestTime(LocalDateTime.now())
                .request(request)
                .contract(null)
                .apiEndpoint(null)
                .apiVersion(null)
                .pathVariables(Collections.emptyMap())
                .queryParams(new java.util.HashMap<>())
                .build();
        ctx.setTenantId("1");

        Ulid128 internalId = new Ulid128(1L, 2L);
        Map<String, Object> ext = new HashMap<>();
        ext.put("storeId", 100L);
        StoreSnapshot storeSnapshot = new StoreSnapshot(
                1L,
                internalId,
                "sto_1",
                "Test Store",
                1,
                true,
                null,
                1L,
                Instant.now(),
                ext
        );
        StoreContext storeContext = new StoreContext(1L, internalId, "sto_1", storeSnapshot);
        ctx.putAttribute("STORE_CONTEXT", storeContext);
        ApiContextHolder.set(ctx);

        when(provider.getOrLoad(anyLong(), any(Ulid128.class), any(String.class), anyLong()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(ctx))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    org.assertj.core.api.Assertions.assertThat(biz.getCode())
                            .isEqualTo(InventoryErrorCode.INVENTORY_POLICY_NOT_FOUND.getCode());
                });

        verify(provider, times(1)).getOrLoad(anyLong(), any(Ulid128.class), any(String.class), anyLong());
    }
}
