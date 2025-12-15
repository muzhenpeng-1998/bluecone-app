package com.bluecone.app.core.cacheinval.api;

import java.time.Instant;
import java.util.List;

/**
 * Cache invalidation event propagated across instances.
 *
 * @param eventId        globally unique ULID string
 * @param tenantId       tenant identifier
 * @param scope          logical invalidation scope
 * @param namespace      cache namespace, aligned with ContextKit CacheKey.namespace
 * @param keys           list of cache key suffixes, e.g. "tenantId:internalId"
 * @param configVersion  optional config version for auditing
 * @param occurredAt     event creation time
 * @param epochBump      whether this event represents a namespace epoch bump
 * @param newEpoch       optional epoch value after bump, for observability
 * @param protectionHint optional protection decision hint (DIRECT/COALESCE/EPOCH_BUMP)
 */
public record CacheInvalidationEvent(
        String eventId,
        long tenantId,
        InvalidationScope scope,
        String namespace,
        List<String> keys,
        long configVersion,
        Instant occurredAt,
        boolean epochBump,
        Long newEpoch,
        String protectionHint
) {

    public CacheInvalidationEvent {
        // keep backward compatibility: default protectionHint to null if blank
        if (protectionHint != null && protectionHint.isBlank()) {
            protectionHint = null;
        }
    }

    public CacheInvalidationEvent(String eventId,
                                  long tenantId,
                                  InvalidationScope scope,
                                  String namespace,
                                  List<String> keys,
                                  long configVersion,
                                  Instant occurredAt) {
        this(eventId, tenantId, scope, namespace, keys, configVersion, occurredAt, false, null, null);
    }
}

