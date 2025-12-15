package com.bluecone.app.infra.notify.support;

import com.bluecone.app.core.notify.NotificationPriority;
import com.bluecone.app.core.notify.NotificationRequest;
import com.bluecone.app.core.notify.NotificationScenario;
import com.bluecone.app.infra.notify.model.NotificationIntent;
import com.bluecone.app.core.tenant.TenantContext;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 构建内部通知意图的辅助类（Support 层）。
 */
public class NotificationContextBuilder {

    public NotificationIntent buildIntent(NotificationRequest request) {
        String traceId = MDC.get("traceId");
        Long tenantId = request.getTenantId() != null ? request.getTenantId() : parseTenantFromContext();
        NotificationPriority priority = request.getPriority();
        if (priority == null) {
            NotificationScenario scenario = NotificationScenario.fromCode(request.getScenarioCode());
            priority = scenario != null ? scenario.getDefaultPriority() : NotificationPriority.NORMAL;
        }
        String idemKey = StringUtils.hasText(request.getIdempotentKey())
                ? request.getIdempotentKey()
                : generateIdempotentKey(request.getScenarioCode(), request.getAttributes());
        return new NotificationIntent(
                request.getScenarioCode(),
                tenantId,
                request.getTriggeredByUserId(),
                priority,
                request.getAttributes(),
                idemKey,
                Instant.now(),
                traceId
        );
    }

    private Long parseTenantFromContext() {
        String tenantId = TenantContext.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return null;
        }
        try {
            return Long.parseLong(tenantId);
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    private String generateIdempotentKey(String scenarioCode, Map<String, Object> attributes) {
        return scenarioCode + ":" + UUID.randomUUID();
    }
}
