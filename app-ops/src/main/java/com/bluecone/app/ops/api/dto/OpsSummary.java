package com.bluecone.app.ops.api.dto;

import java.util.List;

public record OpsSummary(
        String instanceId,
        String appName,
        String version,
        String startedAt,
        SystemSummary system,
        OutboxSummary outbox,
        ConsumeSummary consume,
        IdempotencySummary idempotency,
        CreateSummary create
) {
}

