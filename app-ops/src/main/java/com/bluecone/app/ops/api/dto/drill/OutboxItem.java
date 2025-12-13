package com.bluecone.app.ops.api.dto.drill;

public record OutboxItem(
        long id,
        String eventId,
        String eventType,
        String aggregateType,
        String publicAggregateId,
        String status,
        int retryCount,
        String nextRetryAt,
        String lockedUntil,
        String lockedBy,
        String errorMsg,
        String createdAt
) {
}

