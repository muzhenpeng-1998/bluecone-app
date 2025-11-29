// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/pipeline/steps/MetricsStep.java
package com.bluecone.app.infra.outbox.pipeline.steps;

import com.bluecone.app.infra.outbox.pipeline.OutboxPipeline;
import com.bluecone.app.infra.outbox.pipeline.OutboxPublishContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 指标埋点步骤，占位可接入 Micrometer。
 */
@Component
public class MetricsStep implements OutboxPipeline {

    private static final Logger log = LoggerFactory.getLogger(MetricsStep.class);

    @Override
    public void execute(final OutboxPublishContext context) {
        log.debug("[OutboxMetrics] eventType={} eventId={}", context.getEvent().getEventType(), context.getEvent().getEventId());
    }
}
