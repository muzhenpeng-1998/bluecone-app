package com.bluecone.app.core.cacheinval.transport;

import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;

import java.util.function.Consumer;

/**
 * Abstraction over transport layer for broadcasting cache invalidation events.
 */
public interface CacheInvalidationBus {

    /**
     * Broadcast an event to all interested nodes (including the local one).
     *
     * <p>Implementations must treat this as "at least once" delivery: events may be
     * delivered multiple times and {@link CacheInvalidationEvent} handlers must be
     * idempotent.</p>
     */
    void broadcast(CacheInvalidationEvent event);

    /**
     * Register a local consumer for incoming invalidation events.
     *
     * <p>Implementations should ensure that consumers are invoked asynchronously
     * or on dedicated threads if the underlying transport uses callbacks.</p>
     */
    void subscribe(Consumer<CacheInvalidationEvent> consumer);
}

