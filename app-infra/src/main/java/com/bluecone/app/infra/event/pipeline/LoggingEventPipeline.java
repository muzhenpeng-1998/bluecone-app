// File: app-infra/src/main/java/com/bluecone/app/infra/event/pipeline/LoggingEventPipeline.java
package com.bluecone.app.infra.event.pipeline;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 观测性管道：事件进入编排器时记录日志。
 */
@Component
public class LoggingEventPipeline implements EventPipeline {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPipeline.class);

    @Override
    public DomainEvent process(final DomainEvent event) {
        String traceId = event.getMetadata().get("traceId");
        String tenantId = event.getMetadata().get("tenantId");
        String userId = event.getMetadata().get("userId");
        log.info("[EventIn] type={} id={} at={} traceId={} tenantId={} userId={}",
                event.getEventType(),
                event.getEventId(),
                event.getOccurredAt(),
                traceId,
                tenantId,
                userId);
        return event;
    }
}
