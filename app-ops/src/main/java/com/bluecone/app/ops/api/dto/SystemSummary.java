package com.bluecone.app.ops.api.dto;

public record SystemSummary(
        long uptimeSeconds,
        int threads,
        long heapUsedBytes,
        long heapMaxBytes
) {
}

