package com.bluecone.app.ops;

import com.bluecone.app.ops.config.BlueconeOpsProperties;
import com.bluecone.app.test.AbstractWebIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class OpsSecurityTest extends AbstractWebIntegrationTest {

    @Autowired
    private BlueconeOpsProperties opsProperties;

    @BeforeEach
    void setup() {
        opsProperties.setToken("test-ops-token");
        opsProperties.setAllowLocalhost(false);
        opsProperties.setAllowQueryToken(false);
        opsProperties.setEnabled(false);
    }

    @Test
    void whenDisabled_thenOpsApiReturns404() {
        opsProperties.setEnabled(false);

        ResponseEntity<String> resp = restTemplate.getForEntity(url("/ops/api/summary"), String.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void whenEnabledWithoutToken_thenReturns404() {
        opsProperties.setEnabled(true);

        ResponseEntity<String> resp = restTemplate.getForEntity(url("/ops/api/summary"), String.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void whenEnabledWithValidToken_thenReturns200() {
        opsProperties.setEnabled(true);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Ops-Token", "test-ops-token");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resp = restTemplate.exchange(
                url("/ops/api/summary"), HttpMethod.GET, entity, String.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }
}

