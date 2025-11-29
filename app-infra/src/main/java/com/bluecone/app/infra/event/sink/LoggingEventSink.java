// File: app-infra/src/main/java/com/bluecone/app/infra/event/sink/LoggingEventSink.java
package com.bluecone.app.infra.event.sink;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 将处理完的事件输出到结构化日志的简单 sink。
 *
 * <p>作为观测锚点，未来可与 Kafka/Outbox/审计库等 sink 并行存在，业务代码无需变更。</p>
 */
@Component
public class LoggingEventSink implements EventSink {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventSink.class);

    @Override
    public void deliver(final DomainEvent event) {
        String traceId = event.getMetadata().get("traceId");
        String tenantId = event.getMetadata().get("tenantId");
        String userId = event.getMetadata().get("userId");
        log.info("[EventDelivered] type={} id={} at={} traceId={} tenantId={} userId={}",
                event.getEventType(),
                event.getEventId(),
                event.getOccurredAt(),
                traceId,
                tenantId,
                userId);
    }
}
