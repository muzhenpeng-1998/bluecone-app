package com.bluecone.app.core.cacheinval.guard;

import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;

/**
 * Determines whether an invalidation event should be executed directly,
 * coalesced, or downgraded to epoch bump under storm conditions.
 */
public interface InvalidationStormGuard {

    GuardDecision decide(CacheInvalidationEvent evt);
}

