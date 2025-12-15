package com.bluecone.app.core.cacheinval.coalesce;

import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;

/**
 * Coalesces multiple invalidation events for the same tenant+namespace into
 * fewer, merged events.
 */
public interface InvalidationCoalescer {

    void submit(CacheInvalidationEvent evt);
}

