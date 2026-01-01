package com.bluecone.app.notify.application.webhook;

import com.bluecone.app.notify.dto.webhook.WebhookConfigCreateRequest;
import com.bluecone.app.notify.dto.webhook.WebhookConfigTestRequest;
import com.bluecone.app.notify.dto.webhook.WebhookConfigTestResult;
import com.bluecone.app.notify.dto.webhook.WebhookConfigUpdateRequest;
import com.bluecone.app.notify.dto.webhook.WebhookConfigView;
import com.bluecone.app.core.error.BizErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.infra.webhook.entity.WebhookConfigDO;
import com.bluecone.app.infra.webhook.repository.WebhookConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class WebhookConfigAppService {

    private final WebhookConfigRepository webhookConfigRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public WebhookConfigAppService(WebhookConfigRepository webhookConfigRepository,
                                   WebClient.Builder webClientBuilder,
                                   ObjectMapper objectMapper) {
        this.webhookConfigRepository = webhookConfigRepository;
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * 按当前租户列出所有配置。
     */
    public List<WebhookConfigView> listByCurrentTenant() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException(BizErrorCode.CONTEXT_MISSING);
        }
        Long tenantId = Long.parseLong(tenantIdStr);
        List<WebhookConfigDO> configs = webhookConfigRepository.listByTenant(tenantId);
        return configs.stream().map(this::toView).toList();
    }

    /**
     * 新建 webhook 配置。
     */
    @Transactional
    public WebhookConfigView create(WebhookConfigCreateRequest request) {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException(BizErrorCode.CONTEXT_MISSING);
        }
        Long tenantId = Long.parseLong(tenantIdStr);

        validateEventType(request.getEventType());
        validateUrl(request.getTargetUrl());

        WebhookConfigDO config = new WebhookConfigDO();
        config.setTenantId(tenantId);
        config.setEventType(request.getEventType());
        config.setTargetUrl(request.getTargetUrl());
        String secret = request.getSecret();
        if (!StringUtils.hasText(secret)) {
            secret = generateSecret();
        }
        config.setSecret(secret);
        config.setEnabled(1);
        config.setDescription(request.getDescription());
        LocalDateTime now = LocalDateTime.now();
        config.setCreatedAt(now);
        config.setUpdatedAt(now);

        webhookConfigRepository.save(config);
        return toView(config);
    }

    /**
     * 更新当前租户的配置。
     */
    @Transactional
    public WebhookConfigView update(WebhookConfigUpdateRequest request) {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException(BizErrorCode.CONTEXT_MISSING);
        }
        Long tenantId = Long.parseLong(tenantIdStr);

        WebhookConfigDO config = webhookConfigRepository.findById(request.getId())
                .orElseThrow(() -> new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND));

        if (!tenantId.equals(config.getTenantId())) {
            throw new BusinessException(BizErrorCode.PERMISSION_DENIED);
        }

        if (request.getTargetUrl() != null) {
            validateUrl(request.getTargetUrl());
            config.setTargetUrl(request.getTargetUrl());
        }

        if (request.getSecret() != null) {
            if (request.getSecret().isEmpty()) {
                config.setSecret(null);
            } else {
                config.setSecret(request.getSecret());
            }
        }

        if (request.getEnabled() != null) {
            config.setEnabled(request.getEnabled());
        }

        if (request.getDescription() != null) {
            config.setDescription(request.getDescription());
        }

        config.setUpdatedAt(LocalDateTime.now());
        webhookConfigRepository.update(config);
        return toView(config);
    }

    /**
     * 删除配置（物理删除）。
     */
    @Transactional
    public void delete(Long id) {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException(BizErrorCode.CONTEXT_MISSING);
        }
        Long tenantId = Long.parseLong(tenantIdStr);

        WebhookConfigDO config = webhookConfigRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND));
        if (!tenantId.equals(config.getTenantId())) {
            throw new BusinessException(BizErrorCode.PERMISSION_DENIED);
        }

        webhookConfigRepository.deleteById(id);
    }

    /**
     * 测试 webhook 配置。
     */
    public WebhookConfigTestResult test(WebhookConfigTestRequest request) {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException(BizErrorCode.CONTEXT_MISSING);
        }
        Long tenantId = Long.parseLong(tenantIdStr);

        if (request == null || request.getId() == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "id 不能为空");
        }

        WebhookConfigDO config = webhookConfigRepository.findById(request.getId())
                .orElseThrow(() -> new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND));

        if (!tenantId.equals(config.getTenantId())) {
            throw new BusinessException(BizErrorCode.PERMISSION_DENIED);
        }

        Map<String, Object> testBody = new LinkedHashMap<>();
        testBody.put("type", "WEBHOOK_TEST");
        testBody.put("tenantId", tenantId);
        testBody.put("eventType", config.getEventType());
        testBody.put("timestamp", System.currentTimeMillis());
        if (request.getTestData() != null && !request.getTestData().isEmpty()) {
            testBody.put("data", request.getTestData());
        }

        WebhookConfigTestResult result = new WebhookConfigTestResult();
        try {
            String json = objectMapper.writeValueAsString(testBody);
            WebClient.RequestBodySpec spec = webClient.post()
                    .uri(config.getTargetUrl())
                    .header("Content-Type", "application/json");

            if (StringUtils.hasText(config.getSecret())) {
                spec.header("X-Webhook-Signature", sign(json, config.getSecret()));
            }

            spec.bodyValue(json)
                    .exchangeToMono(response -> response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> {
                                int status = response.statusCode().value();
                                result.setHttpStatus(status);
                                result.setResponseBody(body);
                                boolean success = response.statusCode().is2xxSuccessful();
                                result.setSuccess(success);
                                if (!success) {
                                    result.setErrorMessage("HTTP status: " + status);
                                }
                                return body;
                            }))
                    .block(Duration.ofSeconds(5));
        } catch (Exception ex) {
            result.setSuccess(false);
            result.setErrorMessage(ex.getMessage());
        }

        return result;
    }

    private void validateEventType(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "eventType 不能为空");
        }
    }

    private void validateUrl(String url) {
        if (!StringUtils.hasText(url)) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "targetUrl 不能为空");
        }
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "仅支持 http/https 协议");
        }
    }

    private String generateSecret() {
        return UUID.randomUUID().toString().replace("-", "");
    }

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

    private WebhookConfigView toView(WebhookConfigDO config) {
        WebhookConfigView view = new WebhookConfigView();
        view.setId(config.getId());
        view.setTenantId(config.getTenantId());
        view.setEventType(config.getEventType());
        view.setTargetUrl(config.getTargetUrl());
        view.setEnabled(config.getEnabled());
        view.setDescription(config.getDescription());
        view.setCreatedAt(config.getCreatedAt());
        view.setUpdatedAt(config.getUpdatedAt());
        view.setSecretConfigured(StringUtils.hasText(config.getSecret()));
        return view;
    }
}
