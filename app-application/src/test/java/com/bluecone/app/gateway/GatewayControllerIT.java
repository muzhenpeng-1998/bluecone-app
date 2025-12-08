package com.bluecone.app.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluecone.app.api.auth.dto.LoginResponse;
import com.bluecone.app.core.domain.OrderStatus;
import com.bluecone.app.core.exception.ApiErrorResponse;
import com.bluecone.app.gateway.endpoint.ApiEndpoint;
import com.bluecone.app.gateway.endpoint.ApiVersion;
import com.bluecone.app.gateway.response.ResponseEnvelope;
import com.bluecone.app.test.AbstractWebIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "bluecone.scheduler.enabled=false")
class GatewayControllerIT extends AbstractWebIntegrationTest {

    private static final AtomicBoolean SIGNATURE_ROUTE_REGISTERED = new AtomicBoolean(false);

    @Autowired
    private ApiRouteRegistry apiRouteRegistry;

    @Autowired
    private ApiGatewayProperties gatewayProperties;

    @Autowired
    private SignedTestHandler signedTestHandler;

    @BeforeEach
    void registerSignedRoute() {
        if (SIGNATURE_ROUTE_REGISTERED.compareAndSet(false, true)) {
            apiRouteRegistry.register(ApiContract.builder()
                    .code("test.signed")
                    .httpMethod(org.springframework.http.HttpMethod.GET)
                    .path("/api/gw/tests/signed")
                    .version(ApiVersion.V1.getCode())
                    .authRequired(false)
                    .tenantRequired(false)
                    .signatureRequired(true)
                    .handlerClass(SignedTestHandler.class)
                    .description("Test signed endpoint")
                    .build());
        }
    }

    @Test
    void orderDetailRouteRequiresCorrectVersionAndAuth() {
        HttpHeaders headers = authenticatedHeaders(11L, 5001L);
        headers.add("X-Api-Version", "v1");

        ResponseEntity<ResponseEnvelope<Map<String, Object>>> response = restTemplate.exchange(
                url("/api/gw/orders/42"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("OK");
        assertThat(response.getBody().getData()).containsEntry("orderId", 42);
    }

    @Test
    void invalidVersionFailsFast() {
        HttpHeaders headers = authenticatedHeaders(22L, 3001L);
        headers.add("X-Api-Version", "v2");

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                url("/api/gw/orders/42"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ApiErrorResponse.class);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("UNSUPPORTED_VERSION");
    }

    @Test
    void missingTenantContextIsRejectedForTenantAwareRoutes() {
        HttpHeaders headers = authenticatedHeaders(33L, null);
        headers.add("X-Api-Version", "v1");

        ResponseEntity<ApiErrorResponse> response = restTemplate.exchange(
                url("/api/gw/orders/1"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ApiErrorResponse.class);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("PERMISSION_DENIED");
    }

    @Test
    void signatureMiddlewareValidatesPayload() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        long timestamp = Instant.now().getEpochSecond();
        headers.add("X-Signature-Ts", String.valueOf(timestamp));
        headers.add("X-Signature", hmacSignature("/api/gw/tests/signed", "GET", timestamp));

        ResponseEntity<ResponseEnvelope<Map<String, Object>>> response = restTemplate.exchange(
                url("/api/gw/tests/signed"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("OK");
        assertThat(response.getBody().getData()).containsEntry("result", "signed");

        HttpHeaders missingHeaders = new HttpHeaders();
        ResponseEntity<ApiErrorResponse> invalid = restTemplate.exchange(
                url("/api/gw/tests/signed"),
                HttpMethod.GET,
                new HttpEntity<>(missingHeaders),
                ApiErrorResponse.class);
        assertThat(invalid.getBody()).isNotNull();
        assertThat(invalid.getBody().getCode()).isEqualTo("SIGNATURE_INVALID");
    }

    private String hmacSignature(String path, String method, long timestamp) throws Exception {
        String payload = path + "|" + method + "|" + timestamp;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(gatewayProperties.getSignatureSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(raw.length * 2);
        for (byte b : raw) {
            String hex = Integer.toHexString(b & 0xff);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    static class SignedTestHandler implements ApiHandler<Void, Map<String, Object>> {
        @Override
        public Map<String, Object> handle(ApiContext ctx, Void request) {
            return Map.of("result", "signed");
        }
    }

    @org.springframework.context.annotation.Configuration
    static class GatewayTestConfig {

        @Bean
        SignedTestHandler signedTestHandler() {
            return new SignedTestHandler();
        }
    }
}
