package com.bluecone.app.apicontract;

import com.bluecone.app.Application;
import com.bluecone.app.api.ApiResponse;
import com.bluecone.app.config.StoreContextProperties;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.core.idresolve.api.PublicIdResolver;
import com.bluecone.app.core.idresolve.api.ResolveKey;
import com.bluecone.app.core.idresolve.api.ResolveResult;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.store.runtime.application.StoreSnapshotProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户侧 API 要求必须具备门店上下文的集成测试。
 */
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "bluecone.api.contract.enabled=true",
        "bluecone.api.contract.routes[0].side=USER",
        "bluecone.api.contract.routes[0].includePatterns[0]=/api/mini/**",
        "bluecone.api.contract.routes[0].excludePatterns[0]=/api/mini/public/**",
        "bluecone.api.contract.routes[0].requiredContexts[0]=STORE"
})
class UserApiRequiresStoreTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoreContextProperties storeContextProperties;

    @MockBean
    private PublicIdResolver publicIdResolver;

    @MockBean
    private StoreSnapshotProvider storeSnapshotProvider;

    @RestController
    @RequestMapping("/api/mini/order")
    static class TestOrderController {

        @GetMapping(value = "/confirm", produces = MediaType.APPLICATION_JSON_VALUE)
        public ApiResponse<String> confirm() {
            return ApiResponse.success("ok");
        }
    }

    @BeforeEach
    void setupStoreProps() {
        // 确保门店中间件对 /api/mini/** 生效且必须要求 storeId。
        storeContextProperties.setEnabled(true);
        storeContextProperties.setIncludePaths(java.util.List.of("/api/mini/**"));
        storeContextProperties.setExcludePaths(java.util.List.of());
        storeContextProperties.setRequireStoreId(true);
        storeContextProperties.setAllowMissingStoreIdPaths(java.util.List.of());
    }

    @Test
    void missingStoreIdShouldReturn400() throws Exception {
        mockMvc.perform(get("/api/mini/order/confirm")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.BAD_REQUEST.getCode()));

        verify(publicIdResolver, never()).resolve(any(ResolveKey.class));
        verifyNoInteractions(storeSnapshotProvider);
    }

    @Test
    void invalidStoreIdShouldReturn400() throws Exception {
        String invalidPublicId = "bad_store";
        ResolveResult invalid = new ResolveResult(false, false, null, invalidPublicId,
                ResolveResult.REASON_INVALID_FORMAT);
        when(publicIdResolver.resolve(any(ResolveKey.class))).thenReturn(invalid);

        mockMvc.perform(get("/api/mini/order/confirm")
                        .header("X-Tenant-Id", "1")
                        .header("X-Store-Id", invalidPublicId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.BAD_REQUEST.getCode()));

        ArgumentCaptor<ResolveKey> keyCaptor = ArgumentCaptor.forClass(ResolveKey.class);
        verify(publicIdResolver).resolve(keyCaptor.capture());
        ResolveKey key = keyCaptor.getValue();
        assertThat(key.type()).isEqualTo(ResourceType.STORE);
        assertThat(key.publicId()).isEqualTo(invalidPublicId);

        verifyNoInteractions(storeSnapshotProvider);
    }

    @Test
    void notFoundStoreIdShouldReturn404() throws Exception {
        String publicId = "sto_01NOTFOUND";
        ResolveResult notFound = new ResolveResult(true, false, null, publicId,
                ResolveResult.REASON_NOT_FOUND);
        when(publicIdResolver.resolve(any(ResolveKey.class))).thenReturn(notFound);

        mockMvc.perform(get("/api/mini/order/confirm")
                        .header("X-Tenant-Id", "1")
                        .header("X-Store-Id", publicId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.getCode()));

        verify(publicIdResolver).resolve(any(ResolveKey.class));
        verifyNoInteractions(storeSnapshotProvider);
    }
}
