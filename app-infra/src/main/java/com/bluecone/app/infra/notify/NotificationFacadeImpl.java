package com.bluecone.app.infra.notify;

import com.bluecone.app.core.notify.NotificationFacade;
import com.bluecone.app.core.notify.NotificationRequest;
import com.bluecone.app.core.notify.NotificationResponse;
import com.bluecone.app.infra.notify.config.NotifyProperties;
import com.bluecone.app.infra.notify.outbox.NotifyOutboxPublisher;
import com.bluecone.app.infra.notify.support.NotificationContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

/**
 * NotificationFacade 默认实现（API 层 -> Outbox）。
 */
public class NotificationFacadeImpl implements NotificationFacade {

    private static final Logger log = LoggerFactory.getLogger(NotificationFacadeImpl.class);

    private final NotifyProperties notifyProperties;
    private final NotificationContextBuilder contextBuilder;
    private final NotifyOutboxPublisher notifyOutboxPublisher;

    public NotificationFacadeImpl(NotifyProperties notifyProperties,
                                  NotificationContextBuilder contextBuilder,
                                  NotifyOutboxPublisher notifyOutboxPublisher) {
        this.notifyProperties = Objects.requireNonNull(notifyProperties, "notifyProperties must not be null");
        this.contextBuilder = Objects.requireNonNull(contextBuilder, "contextBuilder must not be null");
        this.notifyOutboxPublisher = Objects.requireNonNull(notifyOutboxPublisher, "notifyOutboxPublisher must not be null");
    }

    @Override
    public NotificationResponse send(NotificationRequest request) {
        if (!notifyProperties.isEnabled()) {
            return NotificationResponse.rejected(UUID.randomUUID().toString(), "NOTIFY_DISABLED");
        }
        var intent = contextBuilder.buildIntent(request);
        boolean accepted = notifyOutboxPublisher.publish(intent);
        if (!accepted) {
            return NotificationResponse.rejected(intent.getIdempotentKey(), "SCENARIO_DISABLED");
        }
        log.info("[NotifyFacade] accepted scenario={} tenant={} traceId={}", intent.getScenarioCode(), intent.getTenantId(), intent.getTraceId());
        return NotificationResponse.accepted(intent.getIdempotentKey());
    }
}
