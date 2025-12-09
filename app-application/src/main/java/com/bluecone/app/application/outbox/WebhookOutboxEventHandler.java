package com.bluecone.app.application.outbox;

import com.bluecone.app.infra.event.outbox.OutboxEventDO;
import com.bluecone.app.infra.webhook.entity.WebhookConfigDO;
import com.bluecone.app.infra.webhook.repository.WebhookConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Webhook Outbox Event Handler：监听 Outbox 事件，查找 Webhook 配置并推送。
 * 支持简单的 HMAC-SHA256 签名验证。
 * 
 * Note: 此为旧版本实现，仅在 dev/test 环境使用。
 */
@Slf4j
@Component
@Profile({"dev", "test"})
public class WebhookOutboxEventHandler implements OutboxEventHandler {

    private final WebhookConfigRepository webhookConfigRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public WebhookOutboxEventHandler(
            WebhookConfigRepository webhookConfigRepository,
            ObjectMapper objectMapper,
            RestTemplateBuilder restTemplateBuilder) {
        this.webhookConfigRepository = webhookConfigRepository;
        this.objectMapper = objectMapper;
        // 配置 RestTemplate，设置连接超时和读取超时
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(java.time.Duration.ofSeconds(5))
                .setReadTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    @Override
    public boolean supports(OutboxEventDO event) {
        // 只处理业务事件（ORDER/PAYMENT），不处理未来可能的系统事件
        String aggType = event.getAggregateType();
        return "ORDER".equalsIgnoreCase(aggType) || "PAYMENT".equalsIgnoreCase(aggType);
    }

    @Override
    public void handle(OutboxEventDO event) throws Exception {
        Long tenantId = event.getTenantId();
        String eventType = event.getEventType();

        if (tenantId == null) {
            log.debug("[Webhook] skip event without tenantId, id={}", event.getId());
            return;
        }

        // 1. 查找租户对应事件类型的 webhook 配置
        Optional<WebhookConfigDO> configOpt = webhookConfigRepository.findEnabledWebhook(tenantId, eventType);
        if (configOpt.isEmpty()) {
            // 没有配置就直接跳过，视为"成功处理"（否则会无限重试）
            log.debug("[Webhook] no config for tenantId={}, eventType={}, eventId={}",
                    tenantId, eventType, event.getId());
            return;
        }

        WebhookConfigDO config = configOpt.get();

        // 2. 构造请求体：建议封一层 envelope
        WebhookPayload payload = buildPayload(event);
        String bodyJson = objectMapper.writeValueAsString(payload);

        // 3. 计算签名（可选简版）
        String signature = null;
        if (config.getSecret() != null && !config.getSecret().isEmpty()) {
            signature = sign(bodyJson, config.getSecret());
        }

        // 4. 发送 HTTP POST
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (signature != null) {
            headers.set("X-Webhook-Signature", signature);
        }

        HttpEntity<String> request = new HttpEntity<>(bodyJson, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    config.getTargetUrl(),
                    HttpMethod.POST,
                    request,
                    String.class
            );

            log.info("[Webhook] success, eventId={}, url={}, status={}, resp={}",
                    event.getId(), config.getTargetUrl(), response.getStatusCode(), response.getBody());
        } catch (Exception ex) {
            log.error("[Webhook] call failed, eventId={}, url={}", event.getId(), config.getTargetUrl(), ex);
            throw new RuntimeException("Webhook call failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 构造 Webhook Payload
     */
    private WebhookPayload buildPayload(OutboxEventDO event) {
        WebhookPayload payload = new WebhookPayload();
        payload.setTenantId(event.getTenantId());
        payload.setAggregateType(event.getAggregateType());
        payload.setAggregateId(event.getAggregateId());
        payload.setEventType(event.getEventType());
        payload.setEventBodyRaw(event.getEventBody());
        payload.setOccurredAt(event.getCreatedAt());
        return payload;
    }

    /**
     * 使用 HMAC-SHA256 计算签名
     */
    private String sign(String body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign webhook payload", e);
        }
    }

    /**
     * 内部 DTO，用于封装 webhook payload
     */
    @Data
    static class WebhookPayload {
        private Long tenantId;
        private String aggregateType;
        private String aggregateId;
        private String eventType;
        private String eventBodyRaw;
        private LocalDateTime occurredAt;
    }
}
