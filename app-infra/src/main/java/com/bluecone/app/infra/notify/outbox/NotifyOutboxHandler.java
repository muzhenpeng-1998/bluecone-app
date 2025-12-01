package com.bluecone.app.infra.notify.outbox;

import com.bluecone.app.core.event.EventHandler;
import com.bluecone.app.infra.notify.delivery.DeliveryResult;
import com.bluecone.app.infra.notify.delivery.NotificationEnvelope;
import com.bluecone.app.infra.notify.delivery.NotificationRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Outbox 消费处理器：执行实际投递。
 */
@Component
public class NotifyOutboxHandler implements EventHandler<NotifyOutboxEvent> {

    private static final Logger log = LoggerFactory.getLogger(NotifyOutboxHandler.class);

    private final NotificationRouter notificationRouter;

    public NotifyOutboxHandler(NotificationRouter notificationRouter) {
        this.notificationRouter = Objects.requireNonNull(notificationRouter, "notificationRouter must not be null");
    }

    @Override
    public void handle(NotifyOutboxEvent event) {
        NotificationEnvelope envelope = new NotificationEnvelope(event.getIntent(), event.getTask());
        DeliveryResult result = notificationRouter.routeAndDeliver(envelope);
        if (!result.isSuccess()) {
            log.warn("[NotifyOutbox] deliver failed eventId={} code={} msg={}", event.getEventId(),
                    result.getErrorCode(), result.getErrorMessage());
            throw new IllegalStateException("Notification delivery failed: " + result.getErrorMessage());
        }
        log.info("[NotifyOutbox] delivered scenario={} channel={} tenant={}",
                event.getIntent().getScenarioCode(), event.getTask().getChannel(), event.getIntent().getTenantId());
    }
}
