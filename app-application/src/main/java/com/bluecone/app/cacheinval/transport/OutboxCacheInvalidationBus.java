package com.bluecone.app.cacheinval.transport;

import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;
import com.bluecone.app.core.cacheinval.transport.CacheInvalidationBus;
import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.event.EventMetadata;
import com.bluecone.app.core.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Outbox-based implementation of {@link CacheInvalidationBus}.
 *
 * <p>Broadcast publishes a {@link CacheInvalidationDomainEvent} via the existing
 * DomainEvent outbox infrastructure. Subscribed consumers are invoked when the
 * event is dispatched by the outbox dispatcher on each node.</p>
 */
public class OutboxCacheInvalidationBus implements CacheInvalidationBus, EventHandler<OutboxCacheInvalidationBus.CacheInvalidationDomainEvent> {

    private static final Logger log = LoggerFactory.getLogger(OutboxCacheInvalidationBus.class);

    private final DomainEventPublisher domainEventPublisher;
    private final List<Consumer<CacheInvalidationEvent>> consumers = new CopyOnWriteArrayList<>();

    public OutboxCacheInvalidationBus(DomainEventPublisher domainEventPublisher) {
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher");
    }

    @Override
    public void broadcast(CacheInvalidationEvent event) {
        if (event == null) {
            return;
        }
        CacheInvalidationDomainEvent domainEvent = CacheInvalidationDomainEvent.from(event);
        domainEventPublisher.publish(domainEvent);
    }

    @Override
    public void subscribe(Consumer<CacheInvalidationEvent> consumer) {
        consumers.add(Objects.requireNonNull(consumer, "consumer"));
    }

    @Override
    public void handle(CacheInvalidationDomainEvent event) {
        CacheInvalidationEvent payload = event.payload();
        for (Consumer<CacheInvalidationEvent> consumer : consumers) {
            try {
                consumer.accept(payload);
            } catch (Exception ex) {
                log.warn("[CacheInvalidation] consumer failed for eventId={}", payload.eventId(), ex);
            }
        }
    }

    /**
     * DomainEvent wrapper for CacheInvalidationEvent so that it can be routed
     * through the existing outbox/event pipeline.
     */
    public static final class CacheInvalidationDomainEvent extends DomainEvent {

        private final CacheInvalidationEvent payload;

        public CacheInvalidationDomainEvent(CacheInvalidationEvent payload, EventMetadata metadata) {
            super("cache.invalidation", metadata);
            this.payload = payload;
        }

        public CacheInvalidationEvent payload() {
            return payload;
        }

        public static CacheInvalidationDomainEvent from(CacheInvalidationEvent event) {
            java.util.Map<String, String> attributes = new java.util.HashMap<>();
            if (event.eventId() != null) {
                attributes.put("eventId", event.eventId());
            }
            attributes.put("eventType", "cache.invalidation");
            attributes.put("tenantId", Long.toString(event.tenantId()));
            attributes.put("scope", event.scope() != null ? event.scope().name() : "UNKNOWN");
            attributes.put("namespace", event.namespace());
            attributes.put("occurredAt", (event.occurredAt() != null ? event.occurredAt() : Instant.now()).toString());
            EventMetadata metadata = attributes.isEmpty() ? EventMetadata.empty() : EventMetadata.of(attributes);
            return new CacheInvalidationDomainEvent(event, metadata);
        }
    }
}
