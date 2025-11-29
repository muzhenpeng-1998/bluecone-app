// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/router/OutboxEventRouter.java
package com.bluecone.app.infra.outbox.router;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventHandler;
import com.bluecone.app.infra.event.router.EventHandlerTypeResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Outbox 投递使用的事件路由器，复用 {@link EventHandler} 生态。
 */
@Component
public class OutboxEventRouter {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventRouter.class);

    private final Map<Class<? extends DomainEvent>, List<EventHandler<?>>> handlerMapping = new ConcurrentHashMap<>();

    public OutboxEventRouter(final List<EventHandler<?>> handlers) {
        Objects.requireNonNull(handlers, "handlers must not be null");
        for (EventHandler<?> handler : handlers) {
            Class<? extends DomainEvent> eventType = EventHandlerTypeResolver.resolveEventType(handler);
            if (eventType == null) {
                continue;
            }
            handlerMapping.computeIfAbsent(eventType, key -> new ArrayList<>()).add(handler);
            log.info("[OutboxRouter] {} handles {}", handler.getClass().getSimpleName(), eventType.getSimpleName());
        }
    }

    public List<EventHandler<?>> route(final DomainEvent event) {
        List<EventHandler<?>> handlers = handlerMapping.get(event.getClass());
        if (handlers == null || handlers.isEmpty()) {
            log.warn("[OutboxRouter] no handler for eventType={} eventId={}", event.getEventType(), event.getEventId());
            return Collections.emptyList();
        }
        return List.copyOf(handlers);
    }
}
