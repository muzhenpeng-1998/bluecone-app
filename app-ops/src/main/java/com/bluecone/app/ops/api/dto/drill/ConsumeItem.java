package com.bluecone.app.ops.api.dto.drill;

public record ConsumeItem(
        long id,
        String consumerGroup,
        String eventId,
        String eventType,
        String status,
        int retryCount,
        String nextRetryAt,
        String lockedUntil,
        String lockedBy,
        String errorMsg,
        String createdAt,
        String updatedAt
) {
}

