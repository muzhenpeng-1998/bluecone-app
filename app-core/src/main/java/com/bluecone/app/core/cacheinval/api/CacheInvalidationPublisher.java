package com.bluecone.app.core.cacheinval.api;

/**
 * Publishes cache invalidation events with after-commit semantics.
 */
public interface CacheInvalidationPublisher {

    /**
     * Publish an invalidation event that will be executed and broadcast
     * after the surrounding transaction (if any) commits.
     *
     * <p>If no transaction is active, implementations may execute and broadcast
     * immediately, but this should be treated as best-effort and documented
     * as less strict than transactional usage.</p>
     *
     * @param event event to publish
     */
    void publishAfterCommit(CacheInvalidationEvent event);
}

