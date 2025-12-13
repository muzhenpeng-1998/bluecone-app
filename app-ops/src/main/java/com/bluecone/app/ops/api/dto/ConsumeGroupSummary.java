package com.bluecone.app.ops.api.dto;

public record ConsumeGroupSummary(
        String group,
        double successRate5m,
        double failureRate5m,
        double avgLatencyMs5m,
        double p95LatencyMs5m
) {
}

