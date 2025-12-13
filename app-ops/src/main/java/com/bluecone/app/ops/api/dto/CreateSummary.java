package com.bluecone.app.ops.api.dto;

public record CreateSummary(
        double successRate5m,
        double failureRate5m,
        double avgTxLatencyMs5m
) {
}

