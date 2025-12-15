package com.bluecone.app.apicontract;

import com.bluecone.app.Application;
import com.bluecone.app.api.ApiResponse;
import com.bluecone.app.application.middleware.InventoryContextResolver;
import com.bluecone.app.application.middleware.StoreContextResolver;
import com.bluecone.app.application.middleware.UserContextResolver;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.ApiContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 平台侧 /api/admin/** 路由不应注入业务上下文。
 */
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "bluecone.api.contract.enabled=true",
        "bluecone.api.contract.routes[0].side=PLATFORM",
        "bluecone.api.contract.routes[0].includePatterns[0]=/api/admin/**"
})
class AdminApiNoContextInjectedTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StoreContextResolver storeContextResolver;

    @MockBean
    private UserContextResolver userContextResolver;

    @MockBean
    private InventoryContextResolver inventoryContextResolver;

    @RestController
    @RequestMapping("/api/admin/contract-test")
    static class AdminTestController {

        @GetMapping("/ctx")
        public ApiResponse<String> ctx() {
            ApiContext ctx = ApiContextHolder.get();
            if (ctx == null) {
                return ApiResponse.success("NO_CTX");
            }
            String side = ctx.getApiSide() != null ? ctx.getApiSide().name() : "NULL";
            int requiredSize = ctx.getRequiredContexts() != null ? ctx.getRequiredContexts().size() : -1;
            int optionalSize = ctx.getOptionalContexts() != null ? ctx.getOptionalContexts().size() : -1;
            return ApiResponse.success(side + "|" + requiredSize + "|" + optionalSize);
        }
    }

    @Test
    void adminApiShouldNotInjectContexts() throws Exception {
        mockMvc.perform(get("/api/admin/contract-test/ctx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("PLATFORM|0|0"));

        verify(storeContextResolver, never()).resolve(any());
        verify(userContextResolver, never()).resolve(any());
        verify(inventoryContextResolver, never()).resolve(any());
    }
}
