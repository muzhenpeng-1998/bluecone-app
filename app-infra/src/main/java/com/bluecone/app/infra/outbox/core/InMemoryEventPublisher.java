// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/core/InMemoryEventPublisher.java
package com.bluecone.app.infra.outbox.core;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.event.EventOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 开发/测试环境的内存发布器，直接在当前线程调度事件。
 *
 * <p>便于本地调试与单测，无需依赖 Outbox 表或调度任务。</p>
 */
@Component
@Profile({"dev", "test"})
public class InMemoryEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEventPublisher.class);

    private final EventOrchestrator eventOrchestrator;

    public InMemoryEventPublisher(final EventOrchestrator eventOrchestrator) {
        this.eventOrchestrator = eventOrchestrator;
    }

    @Override
    public void publish(final DomainEvent event) {
        log.info("[InMemoryEventPublisher] dispatch eventType={} eventId={}", event.getEventType(), event.getEventId());
        eventOrchestrator.fire(event);
    }
}
