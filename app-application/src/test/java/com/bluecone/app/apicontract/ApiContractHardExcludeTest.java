package com.bluecone.app.apicontract;

import com.bluecone.app.Application;
import com.bluecone.app.application.middleware.InventoryContextResolver;
import com.bluecone.app.application.middleware.StoreContextResolver;
import com.bluecone.app.application.middleware.UserContextResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 确认 /ops/** 与 /actuator/** 路径在契约配置错误时也不会被上下文中间件误拦截。
 */
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "bluecone.api.contract.enabled=true",
        "bluecone.api.contract.routes[0].side=USER",
        "bluecone.api.contract.routes[0].includePatterns[0]=/ops/**",
        "bluecone.api.contract.routes[0].requiredContexts[0]=STORE",
        "bluecone.api.contract.routes[0].requiredContexts[1]=USER",
        "bluecone.api.contract.routes[0].requiredContexts[2]=INVENTORY",
        "bluecone.api.contract.routes[1].side=PLATFORM",
        "bluecone.api.contract.routes[1].includePatterns[0]=/actuator/**",
        "bluecone.api.contract.routes[1].requiredContexts[0]=STORE"
})
class ApiContractHardExcludeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StoreContextResolver storeContextResolver;

    @MockBean
    private UserContextResolver userContextResolver;

    @MockBean
    private InventoryContextResolver inventoryContextResolver;

    @RestController
    static class OpsController {

        @GetMapping("/ops/api/summary")
        public String summary() {
            return "ok";
        }
    }

    @Test
    void opsPathShouldBypassContextMiddlewares() throws Exception {
        mockMvc.perform(get("/ops/api/summary"))
                .andExpect(status().isOk());

        verify(storeContextResolver, never()).resolve(any());
        verify(userContextResolver, never()).resolve(any());
        verify(inventoryContextResolver, never()).resolve(any());
    }

    @Test
    void actuatorHealthShouldBypassContextMiddlewares() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        verify(storeContextResolver, never()).resolve(any());
        verify(userContextResolver, never()).resolve(any());
        verify(inventoryContextResolver, never()).resolve(any());
    }
}

