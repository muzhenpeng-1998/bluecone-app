// File: app-infra/src/main/java/com/bluecone/app/infra/event/router/DefaultEventRouter.java
package com.bluecone.app.infra.event.router;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventHandler;
import com.bluecone.app.core.event.EventRouter;
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
 * 应用内的默认事件路由器，将事件映射到 handler Bean。
 *
 * <p>启动时使用 {@link EventHandlerTypeResolver} 构建 {@code eventClass -> handlers} 映射，
 * 基于事件 Class 进行简单路由；未注册事件返回空列表。</p>
 */
@Component
public class DefaultEventRouter implements EventRouter {

    private static final Logger log = LoggerFactory.getLogger(DefaultEventRouter.class);

    private final Map<Class<? extends DomainEvent>, List<EventHandler<?>>> handlerMapping = new ConcurrentHashMap<>();

    public DefaultEventRouter(final List<EventHandler<?>> handlers) {
        Objects.requireNonNull(handlers, "handlers must not be null");
        for (EventHandler<?> handler : handlers) {
            Class<? extends DomainEvent> eventType = EventHandlerTypeResolver.resolveEventType(handler);
            if (eventType == null) {
                log.warn("Skip registering event handler {} because event type cannot be resolved",
                        handler.getClass().getName());
                continue;
            }
            handlerMapping.computeIfAbsent(eventType, key -> new ArrayList<>()).add(handler);
            log.info("Registered event handler {} for event type {}", handler.getClass().getSimpleName(), eventType.getSimpleName());
        }
    }

    @Override
    public List<EventHandler<?>> route(final DomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        List<EventHandler<?>> handlers = new ArrayList<>();
        List<EventHandler<?>> exact = handlerMapping.get(event.getClass());
        if (exact != null) {
            handlers.addAll(exact);
        }
        handlerMapping.forEach((eventType, handlerList) -> {
            if (!eventType.equals(event.getClass()) && eventType.isAssignableFrom(event.getClass())) {
                handlers.addAll(handlerList);
            }
        });
        if (handlers.isEmpty()) {
            log.debug("No handlers registered for eventType={}, eventId={}", event.getEventType(), event.getEventId());
            return Collections.emptyList();
        }
        log.debug("Routing eventType={}, eventId={} to {} handler(s)", event.getEventType(), event.getEventId(), handlers.size());
        return List.copyOf(handlers);
    }
}
