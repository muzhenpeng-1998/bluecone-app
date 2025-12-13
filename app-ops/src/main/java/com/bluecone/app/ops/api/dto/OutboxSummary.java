package com.bluecone.app.ops.api.dto;

import java.util.List;

public record OutboxSummary(
        long ready,
        long processing,
        long failed,
        double oldestAgeSeconds,
        double sendSuccessRate5m,
        double sendFailureRate5m,
        List<String> recentErrors
) {
}

