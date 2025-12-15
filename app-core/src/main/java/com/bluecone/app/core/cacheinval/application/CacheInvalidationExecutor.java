package com.bluecone.app.core.cacheinval.application;

import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;

/**
 * Executes cache invalidation for a given event on the local node.
 */
public interface CacheInvalidationExecutor {

    /**
     * Apply invalidation for the given event against local caches.
     *
     * <p>Implementations must be idempotent: re-executing the same event
     * should not cause errors.</p>
     */
    void execute(CacheInvalidationEvent event);
}

