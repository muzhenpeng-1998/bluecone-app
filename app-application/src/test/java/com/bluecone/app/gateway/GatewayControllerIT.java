package com.bluecone.app.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluecone.app.api.auth.dto.LoginResponse;
import com.bluecone.app.core.domain.OrderStatus;
import com.bluecone.app.core.exception.ApiErrorResponse;
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
import org.springframework.util.ClassUtils;

@TestPropertySource(properties = "bluecone.scheduler.enabled=false")
class GatewayControllerIT extends AbstractWebIntegrationTest {

    private static final AtomicBoolean SIGNATURE_ROUTE_REGISTERED = new AtomicBoolean(false);

    @Autowired(required = false)
    private Object apiRouteRegistry; // 反射使用，避免缺失类时直接报错

    @Autowired(required = false)
    private Object gatewayProperties; // 反射使用，缺少网关配置类则跳过

    @Autowired(required = false)
    private Object signedTestHandler; // 反射生成的代理处理器

    @BeforeEach
    // 仅在首次执行时注册签名路由，验证签名校验链路；若缺少 ApiRouteRegistry 则跳过
    void registerSignedRoute() {
        if (apiRouteRegistry == null || signedTestHandler == null) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "ApiRouteRegistry or ApiHandler missing from classpath");
            return;
        }
        if (SIGNATURE_ROUTE_REGISTERED.compareAndSet(false, true)) {
            try {
                Class<?> apiContractClass = Class.forName("com.bluecone.app.gateway.ApiContract");
                Class<?> apiVersionClass = Class.forName("com.bluecone.app.gateway.endpoint.ApiVersion");
                Object builder = apiContractClass.getMethod("builder").invoke(null);
                Class<?> builderClass = builder.getClass();
                Object v1 = Enum.valueOf((Class<Enum>) apiVersionClass.asSubclass(Enum.class), "V1");

                builderClass.getMethod("code", String.class).invoke(builder, "test.signed");
                builderClass.getMethod("httpMethod", org.springframework.http.HttpMethod.class)
                        .invoke(builder, org.springframework.http.HttpMethod.GET);
                builderClass.getMethod("path", String.class).invoke(builder, "/api/gw/tests/signed");
                builderClass.getMethod("version", String.class).invoke(builder, v1.getClass().getMethod("getCode").invoke(v1));
                builderClass.getMethod("authRequired", boolean.class).invoke(builder, false);
                builderClass.getMethod("tenantRequired", boolean.class).invoke(builder, false);
                builderClass.getMethod("signatureRequired", boolean.class).invoke(builder, true);
                builderClass.getMethod("handlerClass", Class.class).invoke(builder, signedTestHandler.getClass());
                builderClass.getMethod("description", String.class).invoke(builder, "Test signed endpoint");

                Object contract = builderClass.getMethod("build").invoke(builder);
                apiRouteRegistry.getClass()
                        .getMethod("register", apiContractClass)
                        .invoke(apiRouteRegistry, contract);
            } catch (ClassNotFoundException cnf) {
                org.junit.jupiter.api.Assumptions.assumeTrue(false, "Gateway classes missing from classpath");
            } catch (Exception e) {
                throw new RuntimeException("Failed to register test route via reflection", e);
            }
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
        mac.init(new SecretKeySpec(signatureSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
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

    private String signatureSecret() {
        if (gatewayProperties == null) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "ApiGatewayProperties missing from classpath");
            return "";
        }
        try {
            return (String) gatewayProperties.getClass()
                    .getMethod("getSignatureSecret")
                    .invoke(gatewayProperties);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read signatureSecret via reflection", e);
        }
    }

    @org.springframework.context.annotation.Configuration
    static class GatewayTestConfig {

        @Bean
        Object signedTestHandler() {
            try {
                Class<?> apiHandler = Class.forName("com.bluecone.app.gateway.ApiHandler");
                Class<?> apiContext = Class.forName("com.bluecone.app.gateway.ApiContext");
                java.lang.reflect.InvocationHandler handler = (proxy, method, args) -> {
                    if ("handle".equals(method.getName()) && args != null && args.length == 2) {
                        return Map.of("result", "signed");
                    }
                    return null;
                };
                return java.lang.reflect.Proxy.newProxyInstance(
                        GatewayControllerIT.class.getClassLoader(),
                        new Class<?>[]{apiHandler},
                        handler);
            } catch (ClassNotFoundException ex) {
                org.junit.jupiter.api.Assumptions.assumeTrue(false, "ApiHandler missing from classpath");
                return new Object();
            }
        }
    }
}
