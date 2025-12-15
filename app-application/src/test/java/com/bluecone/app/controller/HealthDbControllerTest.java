package com.bluecone.app.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluecone.app.infra.entity.TestEntity;
import com.bluecone.app.infra.mapper.TestMapper;
import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.test.AbstractWebIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

class HealthDbControllerTest extends AbstractWebIntegrationTest {

    @Autowired
    private TestMapper testMapper;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        flushRedis();
        TenantContext.setTenantId("tenant-health");
        TestEntity entity = new TestEntity();
        entity.setName("seed");
        testMapper.insert(entity);
        TenantContext.clear();
    }

    @Test
    void dbHealthEndpointReturnsTenantContext() {
        HttpHeaders headers = authenticatedHeaders(88L, 4004L);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url("/health/db"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("ok");
        assertThat(response.getBody().get("tenantId")).isEqualTo("4004");
    }
}
