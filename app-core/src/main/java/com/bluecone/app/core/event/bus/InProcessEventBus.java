package com.bluecone.app.core.event.bus;

import com.bluecone.app.core.event.consume.api.ConsumeOptions;
import com.bluecone.app.core.event.consume.api.EventEnvelope;
import com.bluecone.app.core.event.consume.api.EventHandler;
import com.bluecone.app.core.event.consume.api.EventHandlerTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 简单的进程内事件总线，仅用于开发与测试环境。
 *
 * <p>通过 {@link EventHandlerTemplate} 统一走消费端幂等与重试逻辑。</p>
 */
public class InProcessEventBus {

    private final EventHandlerTemplate handlerTemplate;
    private final ConsumeOptions defaultOptions;

    private final Map<String, Map<String, List<EventHandler>>> registry = new ConcurrentHashMap<>();

    public InProcessEventBus(EventHandlerTemplate handlerTemplate) {
        this(handlerTemplate, new ConsumeOptions(
                Duration.ofSeconds(30),
                false,
                Duration.ZERO,
                20,
                Duration.ofSeconds(1),
                Duration.ofMinutes(5)
        ));
    }

    public InProcessEventBus(EventHandlerTemplate handlerTemplate, ConsumeOptions defaultOptions) {
        this.handlerTemplate = Objects.requireNonNull(handlerTemplate, "handlerTemplate must not be null");
        this.defaultOptions = Objects.requireNonNull(defaultOptions, "defaultOptions must not be null");
    }

    public void register(String consumerGroup, String eventType, EventHandler handler) {
        registry
                .computeIfAbsent(consumerGroup, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(handler);
    }

    public void publish(EventEnvelope event) {
        registry.forEach((group, byType) -> {
            List<EventHandler> handlers = byType.get(event.eventType());
            if (handlers == null || handlers.isEmpty()) {
                return;
            }
            for (EventHandler handler : handlers) {
                handlerTemplate.consume(group, event, handler, defaultOptions);
            }
        });
    }
}

