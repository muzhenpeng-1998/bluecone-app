package com.bluecone.app.core.cacheinval.guard;

/**
 * Decision type for handling a cache invalidation event under protection.
 */
public enum InvalidationDecision {

    /**
     * Execute invalidation for the provided keys directly and broadcast as-is.
     */
    DIRECT_KEYS,

    /**
     * Coalesce events for the same tenant+namespace in a short window.
     */
    COALESCE,

    /**
     * Bump namespace epoch for the tenant and avoid per-key invalidation.
     */
    EPOCH_BUMP
}

