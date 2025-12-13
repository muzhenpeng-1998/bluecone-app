package com.bluecone.app.ops.api.dto;

public record IdempotencySummary(
        double conflictRate10m,
        double inProgressRate5m,
        double avgLatencyMs5m
) {
}

