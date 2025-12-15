package com.bluecone.app.core.cacheinval.observability.api;

import java.time.Instant;
import java.util.List;

/**
 * Log entry for cache invalidation events used by ops observability.
 */
public record CacheInvalidationLogEntry(
        Instant occurredAt,
        Instant receivedAt,
        long tenantId,
        String scope,
        String namespace,
        String eventId,
        int keysCount,
        List<String> keySampleHashes,
        Long configVersion,
        String transport,
        String instanceId,
        String result,
        String note,
        String decision,
        boolean stormMode,
        Long epoch
) {
}
