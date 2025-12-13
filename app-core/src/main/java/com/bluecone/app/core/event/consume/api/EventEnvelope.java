package com.bluecone.app.core.event.consume.api;

import com.bluecone.app.id.core.Ulid128;

import java.time.Instant;

/**
 * 事件消费 Envelope，用于在消费端传递必要的上下文信息。
 *
 * <p>与 Outbox 持久化结构解耦，只保留消费端需要的字段。</p>
 */
public record EventEnvelope(
        long tenantId,
        Ulid128 eventId,
        String eventType,
        String payloadJson,
        String headersJson,
        Instant occurredAt
) {
}

