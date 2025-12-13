package com.bluecone.app.ops.api.dto;

import java.util.List;

public record ConsumeSummary(
        List<ConsumeGroupSummary> groups,
        long retryReady,
        long inProgressLocks
) {
}

