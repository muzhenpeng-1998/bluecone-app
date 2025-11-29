// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/pipeline/steps/ValidateStep.java
package com.bluecone.app.infra.outbox.pipeline.steps;

import com.bluecone.app.infra.outbox.pipeline.OutboxPipeline;
import com.bluecone.app.infra.outbox.pipeline.OutboxPublishContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 校验步骤：确保事件合法、字段齐备。
 */
@Component
public class ValidateStep implements OutboxPipeline {

    @Override
    public void execute(final OutboxPublishContext context) {
        if (!StringUtils.hasText(context.getEvent().getEventType())) {
            throw new IllegalArgumentException("eventType must not be blank");
        }
    }
}
