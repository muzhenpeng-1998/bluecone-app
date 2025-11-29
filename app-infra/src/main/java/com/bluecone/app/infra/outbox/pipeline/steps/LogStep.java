// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/pipeline/steps/LogStep.java
package com.bluecone.app.infra.outbox.pipeline.steps;

import com.bluecone.app.infra.outbox.pipeline.OutboxPipeline;
import com.bluecone.app.infra.outbox.pipeline.OutboxPublishContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 日志记录步骤：入站观测。
 */
@Component
public class LogStep implements OutboxPipeline {

    private static final Logger log = LoggerFactory.getLogger(LogStep.class);

    @Override
    public void execute(final OutboxPublishContext context) {
        log.info("[OutboxIn] eventType={} eventId={} headers={}",
                context.getEvent().getEventType(),
                context.getEvent().getEventId(),
                context.getHeaders());
    }
}
