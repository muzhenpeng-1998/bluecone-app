package com.bluecone.app.infra.notify;

import com.bluecone.app.core.notify.NotificationFacade;
import com.bluecone.app.core.notify.NotificationPriority;
import com.bluecone.app.core.notify.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 通用告警服务：优先通过 NotificationFacade 投递，无配置时降级为日志告警。
 */
@Component
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    private static final String PAYMENT_ALERT_SCENARIO = "PAYMENT_ALERT";

    private final NotificationFacade notificationFacade;

    public AlertService(@Nullable final NotificationFacade notificationFacade) {
        this.notificationFacade = notificationFacade;
    }

    public void sendPaymentAlarm(final String title, final String content) {
        if (notificationFacade == null) {
            log.warn("[PAYMENT-ALARM] {} - {}", title, content);
            return;
        }
        NotificationRequest request = new NotificationRequest();
        request.setScenarioCode(PAYMENT_ALERT_SCENARIO);
        request.setPriority(NotificationPriority.HIGH);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("title", title);
        attrs.put("content", content);
        request.setAttributes(attrs);
        request.setIdempotentKey(UUID.randomUUID().toString());
        try {
            notificationFacade.send(request);
        } catch (Exception ex) {
            log.warn("[PAYMENT-ALARM] fallback log due to notify failure, title={}, content={}, error={}",
                    title, content, ex.getMessage());
            log.warn("[PAYMENT-ALARM] {} - {}", title, content);
        }
    }
}
