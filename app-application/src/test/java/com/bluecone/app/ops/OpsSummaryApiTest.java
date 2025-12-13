package com.bluecone.app.ops;

import com.bluecone.app.ops.config.BlueconeOpsProperties;
import com.bluecone.app.test.AbstractWebIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class OpsSummaryApiTest extends AbstractWebIntegrationTest {

    @Autowired
    private BlueconeOpsProperties opsProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        opsProperties.setEnabled(true);
        opsProperties.setToken("test-ops-token");
        opsProperties.setAllowLocalhost(false);
        opsProperties.setAllowQueryToken(false);
    }

    @Test
    void summaryApiReturnsExpectedStructure() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Ops-Token", "test-ops-token");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resp = restTemplate.exchange(
                url("/ops/api/summary"), HttpMethod.GET, entity, String.class);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isNotNull();

        JsonNode root = objectMapper.readTree(resp.getBody());
        assertThat(root.has("outbox")).isTrue();
        assertThat(root.has("consume")).isTrue();
        assertThat(root.has("system")).isTrue();
    }
}

