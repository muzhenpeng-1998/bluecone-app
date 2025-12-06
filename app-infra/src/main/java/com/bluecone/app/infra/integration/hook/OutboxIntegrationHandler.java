package com.bluecone.app.infra.integration.hook;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventHandler;
import com.bluecone.app.core.integration.IntegrationFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Outbox 回调集成入口：监听领域事件并交给 Integration Hub。
 */
@Component
public class OutboxIntegrationHandler implements EventHandler<DomainEvent> {

    private static final Logger log = LoggerFactory.getLogger(OutboxIntegrationHandler.class);

    private final IntegrationFacade integrationFacade;

    public OutboxIntegrationHandler(final IntegrationFacade integrationFacade) {
        this.integrationFacade = Objects.requireNonNull(integrationFacade, "integrationFacade must not be null");
    }

    @Override
    public void handle(final DomainEvent event) {
        log.info("[Integration][Hook] receive eventId={} type={}", event.getEventId(), event.getEventType());
        integrationFacade.publishIntegration(event);
    }
}
