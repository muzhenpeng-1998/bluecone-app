// File: app-infra/src/main/java/com/bluecone/app/infra/event/pipeline/MetricsEventPipeline.java
package com.bluecone.app.infra.event.pipeline;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 事件指标的占位管道。
 *
 * <p>当前仅记录 debug 日志。未来可接入 Micrometer，将事件类型作为维度计数：
 * {@code bluecone.event.count{eventType="order.paid"}}，便于看板与告警。</p>
 */
@Component
public class MetricsEventPipeline implements EventPipeline {

    private static final Logger log = LoggerFactory.getLogger(MetricsEventPipeline.class);

    @Override
    public DomainEvent process(final DomainEvent event) {
        log.debug("[EventMetrics] eventType={} eventId={}", event.getEventType(), event.getEventId());
        return event;
    }
}
