package com.bluecone.app.application.middleware;

import com.bluecone.app.Application;
import com.bluecone.app.config.StoreContextProperties;
import com.bluecone.app.core.idresolve.api.PublicIdResolver;
import com.bluecone.app.core.idresolve.api.ResolveKey;
import com.bluecone.app.core.idresolve.api.ResolveResult;
import com.bluecone.app.gateway.context.ApiContextHolder;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.store.runtime.api.StoreContext;
import com.bluecone.app.store.runtime.api.StoreSnapshot;
import com.bluecone.app.store.runtime.application.StoreSnapshotProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 门店上下文中间件 Happy Path：解析 publicId 并注入 ApiContext。
 */
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class StoreContextMiddlewareHappyPathTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoreContextProperties props;

    @MockBean
    private PublicIdResolver publicIdResolver;

    @MockBean
    private StoreSnapshotProvider snapshotProvider;

    @RestController
    static class DummyController {
        @GetMapping("/api/mini/test-store")
        public String hello() {
            StoreContext ctx = (StoreContext) ApiContextHolder.get().getAttribute("STORE_CONTEXT");
            return ctx != null ? ctx.storePublicId() : "none";
        }
    }

    @BeforeEach
    void setupProps() {
        props.setIncludePaths(java.util.List.of("/api/mini/**"));
        props.setExcludePaths(java.util.List.of());
        props.setRequireStoreId(true);
        props.setAllowMissingStoreIdPaths(java.util.List.of());
    }

    @Test
    void shouldInjectStoreContextIntoApiContext() throws Exception {
        String publicId = "sto_01TEST";
        Ulid128 internalId = new Ulid128(1L, 2L);
        ResolveResult resolveResult = new ResolveResult(true, true, internalId, publicId, "HIT_DB");
        when(publicIdResolver.resolve(any(ResolveKey.class))).thenReturn(resolveResult);

        StoreSnapshot snapshot = new StoreSnapshot(
                1L,
                internalId,
                publicId,
                "Test Store",
                1,
                true,
                null,
                1L,
                Instant.now(),
                Map.of()
        );
        when(snapshotProvider.getOrLoad(1L, internalId, publicId)).thenReturn(Optional.of(snapshot));

        mockMvc.perform(get("/api/mini/test-store")
                        .header("X-Tenant-Id", "1")
                        .header("X-Store-Id", publicId))
                .andExpect(status().isOk());

        ArgumentCaptor<ResolveKey> keyCaptor = ArgumentCaptor.forClass(ResolveKey.class);
        org.mockito.Mockito.verify(publicIdResolver).resolve(keyCaptor.capture());
        ResolveKey key = keyCaptor.getValue();
        assertThat(key.tenantId()).isEqualTo(1L);
        assertThat(key.type()).isEqualTo(ResourceType.STORE);
        assertThat(key.publicId()).isEqualTo(publicId);
    }
}

