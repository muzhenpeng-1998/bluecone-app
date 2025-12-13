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

class DrillApiValidationTest extends AbstractWebIntegrationTest {

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
        opsProperties.setMaxPageSize(5);
    }

    private HttpHeaders opsHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Ops-Token", "test-ops-token");
        return headers;
    }

    @Test
    void limitGreaterThanMaxPageSizeIsClamped() throws Exception {
        HttpEntity<Void> entity = new HttpEntity<>(opsHeaders());
        ResponseEntity<String> resp = restTemplate.exchange(
                url("/ops/api/outbox?status=FAILED&limit=50"), HttpMethod.GET, entity, String.class);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode root = objectMapper.readTree(resp.getBody());
        assertThat(root.get("limit").asInt()).isEqualTo(5);
    }

    @Test
    void invalidOutboxStatusReturns400() {
        HttpEntity<Void> entity = new HttpEntity<>(opsHeaders());
        ResponseEntity<String> resp = restTemplate.exchange(
                url("/ops/api/outbox?status=BAD&limit=10"), HttpMethod.GET, entity, String.class);

        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void invalidConsumeGroupReturns400() {
        HttpEntity<Void> entity = new HttpEntity<>(opsHeaders());
        ResponseEntity<String> resp = restTemplate.exchange(
                url("/ops/api/consume?group=bad-group!&status=FAILED&limit=10"), HttpMethod.GET, entity, String.class);

        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }
}

