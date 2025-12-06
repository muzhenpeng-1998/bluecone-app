package com.bluecone.app.infra.integration.channel;

import com.bluecone.app.infra.integration.domain.IntegrationChannelType;
import com.bluecone.app.infra.integration.entity.IntegrationDeliveryEntity;
import com.bluecone.app.infra.integration.entity.IntegrationSubscriptionEntity;
import com.bluecone.app.infra.integration.model.IntegrationDeliveryResult;
import com.bluecone.app.infra.integration.support.IntegrationHttpClient;
import com.bluecone.app.infra.integration.support.IntegrationSignatureUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 通用 HTTP Webhook 通道。
 */
@Component
public class WebhookChannel implements IntegrationChannel {

    private static final Logger log = LoggerFactory.getLogger(WebhookChannel.class);

    private final IntegrationHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public WebhookChannel(final IntegrationHttpClient httpClient,
                          final ObjectMapper objectMapper) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public IntegrationChannelType type() {
        return IntegrationChannelType.WEBHOOK;
    }

    @Override
    public IntegrationDeliveryResult send(final IntegrationDeliveryEntity delivery,
                                          final IntegrationSubscriptionEntity subscription) {
        Objects.requireNonNull(delivery, "delivery must not be null");
        Objects.requireNonNull(subscription, "subscription must not be null");
        long start = System.currentTimeMillis();
        if (subscription.getTargetUrl() == null || subscription.getTargetUrl().isBlank()) {
            return IntegrationDeliveryResult.failure("TARGET_URL_MISSING", "targetUrl is blank", null, 0L);
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("eventId", delivery.getEventId());
            body.put("eventType", delivery.getEventType());
            body.put("tenantId", delivery.getTenantId());
            body.put("payload", parsePayload(delivery.getPayload()));
            body.put("metadata", buildMetadata(delivery.getHeaders()));

            String jsonBody = objectMapper.writeValueAsString(body);
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String signature = IntegrationSignatureUtil.sign(jsonBody, timestamp, subscription.getSecret());
            Map<String, String> headers = new HashMap<>();
            headers.put("X-BC-Timestamp", timestamp);
            headers.put("X-BC-Signature", signature);
            headers.put("X-BC-Event-Type", delivery.getEventType());
            headers.put("X-BC-Event-Id", delivery.getEventId());
            if (delivery.getTenantId() != null) {
                headers.put("X-BC-Tenant-Id", String.valueOf(delivery.getTenantId()));
            }
            headers.put("Content-Type", "application/json");
            mergeExtraHeaders(headers, subscription.getHeaders());

            int timeout = subscription.getTimeoutMs() == null ? 3000 : subscription.getTimeoutMs();
            IntegrationDeliveryResult result = httpClient.postJson(subscription.getTargetUrl(), headers, jsonBody, timeout);
            long duration = System.currentTimeMillis() - start;
            log.info("[Integration][Webhook] send eventId={} subscription={} tenant={} success={} status={} durationMs={}",
                    delivery.getEventId(), subscription.getId(), delivery.getTenantId(),
                    result.isSuccess(), result.getHttpStatus(), duration);
            return result;
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;
            log.warn("[Integration][Webhook] send failed eventId={} subscription={} tenant={} error={}",
                    delivery.getEventId(), subscription.getId(), delivery.getTenantId(), ex.getMessage());
            return IntegrationDeliveryResult.failure("WEBHOOK_SEND_ERROR", ex.getMessage(), null, duration);
        }
    }

    private Object parsePayload(final String payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, JsonNode.class);
        } catch (Exception ex) {
            return payload;
        }
    }

    private Map<String, Object> buildMetadata(final String headersJson) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "bluecone");
        if (headersJson == null || headersJson.isBlank()) {
            return metadata;
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(headersJson, new TypeReference<Map<String, Object>>() {
            });
            metadata.putAll(parsed);
        } catch (Exception ignore) {
            metadata.put("rawHeaders", headersJson);
        }
        return metadata;
    }

    private void mergeExtraHeaders(final Map<String, String> headers, final String extraHeadersJson) {
        if (extraHeadersJson == null || extraHeadersJson.isBlank()) {
            return;
        }
        try {
            Map<String, String> extra = objectMapper.readValue(extraHeadersJson, new TypeReference<Map<String, String>>() {
            });
            headers.putAll(extra);
        } catch (Exception ex) {
            log.warn("[Integration][Webhook] invalid headers config: {}", ex.getMessage());
        }
    }
}
