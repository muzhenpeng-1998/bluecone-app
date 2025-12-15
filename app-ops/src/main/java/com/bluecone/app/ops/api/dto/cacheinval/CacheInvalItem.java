package com.bluecone.app.ops.api.dto.cacheinval;

public record CacheInvalItem(
        long id,
        String occurredAt,
        long tenantId,
        String scope,
        String namespace,
        String eventId,
        int keysCount,
        String keySamples,
        Long configVersion,
        String transport,
        String instanceId,
        String result,
        String decision,
        boolean stormMode,
        Long epoch
) {
}
