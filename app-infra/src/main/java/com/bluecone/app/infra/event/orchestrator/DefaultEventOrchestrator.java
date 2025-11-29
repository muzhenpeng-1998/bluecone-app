// File: app-infra/src/main/java/com/bluecone/app/infra/event/orchestrator/DefaultEventOrchestrator.java
package com.bluecone.app.infra.event.orchestrator;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventHandler;
import com.bluecone.app.core.event.EventOrchestrator;
import com.bluecone.app.core.event.EventPipeline;
import com.bluecone.app.core.event.EventRouter;
import com.bluecone.app.core.event.EventSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 应用内同步的事件编排器，串联 pipeline、router、handler、sink。
 *
 * <p>目前是 {@link EventOrchestrator} 的唯一实现，未来可引入异步分发、线程池、重试、
 * Outbox 或 Saga 编排，而业务侧调用保持不变。</p>
 */
@Component
public class DefaultEventOrchestrator implements EventOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DefaultEventOrchestrator.class);

    private final List<EventPipeline> pipelines;
    private final EventRouter eventRouter;
    private final List<EventSink> sinks;

    public DefaultEventOrchestrator(final List<EventPipeline> pipelines,
                                    final EventRouter eventRouter,
                                    final List<EventSink> sinks) {
        this.pipelines = Objects.requireNonNullElseGet(pipelines, List::of);
        this.eventRouter = Objects.requireNonNull(eventRouter, "eventRouter must not be null");
        this.sinks = Objects.requireNonNullElseGet(sinks, List::of);
    }

    @Override
    public void fire(final DomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        DomainEvent processed = event;

        for (EventPipeline pipeline : pipelines) {
            processed = pipeline.process(processed);
        }

        List<EventHandler<?>> handlers = eventRouter.route(processed);
        for (EventHandler<?> handler : handlers) {
            invokeHandler(handler, processed);
        }

        for (EventSink sink : sinks) {
            sink.deliver(processed);
        }
    }

    @SuppressWarnings("unchecked")
    private <E extends DomainEvent> void invokeHandler(final EventHandler<?> handler, final DomainEvent event) {
        try {
            EventHandler<E> typedHandler = (EventHandler<E>) handler;
            typedHandler.handle((E) event);
        } catch (Exception ex) {
            log.error("Event handler {} failed for eventType={}, eventId={}",
                    handler.getClass().getSimpleName(), event.getEventType(), event.getEventId(), ex);
            throw new EventDispatchException("Event handler failed: " + handler.getClass().getSimpleName(), ex);
        }
    }
}
