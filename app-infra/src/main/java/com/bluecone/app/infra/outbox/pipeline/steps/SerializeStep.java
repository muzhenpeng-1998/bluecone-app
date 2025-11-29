// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/pipeline/steps/SerializeStep.java
package com.bluecone.app.infra.outbox.pipeline.steps;

import com.bluecone.app.infra.outbox.core.EventSerializer;
import com.bluecone.app.infra.outbox.pipeline.OutboxPipeline;
import com.bluecone.app.infra.outbox.pipeline.OutboxPublishContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 序列化步骤：生成 JSON 载荷与头部。
 */
@Component
public class SerializeStep implements OutboxPipeline {

    private final EventSerializer serializer;

    public SerializeStep(final EventSerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public void execute(final OutboxPublishContext context) {
        String payload = serializer.serializePayload(context.getEvent());
        Map<String, String> headers = serializer.serializeHeaders(context.getEvent());
        headers.putIfAbsent("eventKey", context.getEvent().getEventId());
        headers.putIfAbsent("eventId", context.getEvent().getEventId());
        headers.putIfAbsent("occurredAt", context.getEvent().getOccurredAt().toString());
        context.setPayload(payload);
        context.setHeaders(headers);
    }
}
