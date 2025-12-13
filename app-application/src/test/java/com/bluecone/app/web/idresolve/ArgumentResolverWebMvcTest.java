package com.bluecone.app.web.idresolve;

import com.bluecone.app.core.idresolve.api.PublicIdResolver;
import com.bluecone.app.core.idresolve.api.ResolveKey;
import com.bluecone.app.core.idresolve.api.ResolvePublicId;
import com.bluecone.app.core.idresolve.api.ResolveResult;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.infra.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 基于 WebMvcTest 的公共 ID 参数解析集成测试。
 */
@WebMvcTest(controllers = ArgumentResolverWebMvcTest.TestController.class)
class ArgumentResolverWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicIdResolver publicIdResolver;

    @BeforeEach
    void setupTenant() {
        TenantContext.setTenantId("1");
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @RestController
    @RequestMapping("/test/idresolve")
    static class TestController {

        @GetMapping("/store")
        public String getStore(@RequestParam("storeId") @ResolvePublicId(type = ResourceType.STORE) Ulid128 storeInternalId) {
            return storeInternalId.toString();
        }
    }

    @Test
    void shouldResolvePublicIdToUlid128() throws Exception {
        String publicId = "sto_01TESTPUBLICID";
        Ulid128 internalId = new Ulid128(123L, 456L);
        ResolveResult result = new ResolveResult(true, true, internalId, publicId, "HIT_DB");
        when(publicIdResolver.resolve(any(ResolveKey.class))).thenReturn(result);

        mockMvc.perform(get("/test/idresolve/store").param("storeId", publicId))
                .andExpect(status().isOk())
                .andExpect(content().string(internalId.toString()));

        ArgumentCaptor<ResolveKey> keyCaptor = ArgumentCaptor.forClass(ResolveKey.class);
        verify(publicIdResolver).resolve(keyCaptor.capture());
        ResolveKey key = keyCaptor.getValue();
        assertThat(key.tenantId()).isEqualTo(1L);
        assertThat(key.type()).isEqualTo(ResourceType.STORE);
        assertThat(key.publicId()).isEqualTo(publicId);
    }

    @Test
    void notFoundShouldReturn404() throws Exception {
        String publicId = "sto_01NOTFOUND";
        ResolveResult notFound = new ResolveResult(true, false, null, publicId, ResolveResult.REASON_NOT_FOUND);
        when(publicIdResolver.resolve(any(ResolveKey.class))).thenReturn(notFound);

        mockMvc.perform(get("/test/idresolve/store").param("storeId", publicId))
                .andExpect(status().isNotFound());
    }
}

