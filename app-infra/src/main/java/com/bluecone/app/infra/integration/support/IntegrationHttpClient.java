package com.bluecone.app.infra.integration.support;

import com.bluecone.app.infra.integration.model.IntegrationDeliveryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * 轻量 HTTP 客户端封装，便于统一超时与日志。
 */
public class IntegrationHttpClient {

    private static final Logger log = LoggerFactory.getLogger(IntegrationHttpClient.class);

    private final RestTemplateBuilder restTemplateBuilder;

    public IntegrationHttpClient(final RestTemplateBuilder restTemplateBuilder) {
        this.restTemplateBuilder = Objects.requireNonNull(restTemplateBuilder, "restTemplateBuilder must not be null");
    }

    /**
     * 发送 JSON POST 请求。
     *
     * @param url       目标地址
     * @param headers   额外头信息
     * @param body      JSON 字符串
     * @param timeoutMs 超时时间
     * @return 投递结果
     */
    public IntegrationDeliveryResult postJson(final String url,
                                              final Map<String, String> headers,
                                              final String body,
                                              final int timeoutMs) {
        RestTemplate restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        if (headers != null) {
            headers.forEach(httpHeaders::add);
        }

        HttpEntity<String> entity = new HttpEntity<>(body, httpHeaders);
        long start = System.currentTimeMillis();
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            long duration = System.currentTimeMillis() - start;
            if (response.getStatusCode().is2xxSuccessful()) {
                return IntegrationDeliveryResult.success(response.getStatusCode().value(), duration);
            }
            String truncated = response.getBody();
            if (truncated != null && truncated.length() > 512) {
                truncated = truncated.substring(0, 512);
            }
            return IntegrationDeliveryResult.failure("HTTP_STATUS_" + response.getStatusCode().value(),
                    truncated,
                    response.getStatusCode().value(),
                    duration);
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;
            log.warn("[IntegrationHttp] request failed url={} error={}", url, ex.getMessage());
            return IntegrationDeliveryResult.failure("HTTP_SEND_ERROR", ex.getMessage(), null, duration);
        }
    }
}
