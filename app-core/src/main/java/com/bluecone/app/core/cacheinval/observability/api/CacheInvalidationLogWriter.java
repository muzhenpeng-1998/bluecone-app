package com.bluecone.app.core.cacheinval.observability.api;

/**
 * SPI for writing cache invalidation logs to an observability sink (e.g. DB).
 */
public interface CacheInvalidationLogWriter {

    /**
     * Persist a log entry. Implementations must not throw exceptions that break
     * the main flow; failures should be logged and swallowed.
     */
    void write(CacheInvalidationLogEntry entry);

    /**
     * No-op implementation used when no concrete writer is available.
     */
    CacheInvalidationLogWriter NOOP = entry -> {
        // no-op
    };
}

