package com.bluecone.app.core.cacheepoch.api;

/**
 * Provides per-namespace cache epoch for building cache keys and performing
 * epoch-based invalidation.
 */
public interface CacheEpochProvider {

    /**
     * Get the current epoch for the given tenant and namespace.
     *
     * @param tenantId  tenant identifier
     * @param namespace cache namespace (e.g. "store:snap")
     * @return current epoch value (>= 1)
     */
    long currentEpoch(long tenantId, String namespace);

    /**
     * Atomically bump the epoch for the given tenant and namespace.
     *
     * @param tenantId  tenant identifier
     * @param namespace cache namespace
     * @return new epoch value after increment
     */
    long bumpEpoch(long tenantId, String namespace);

    /**
     * Update the local cached epoch for the given tenant and namespace,
     * typically based on an epoch bump event received from another instance.
     *
     * <p>Implementations should only adjust in-memory state and must not
     * decrease the effective epoch.</p>
     */
    void updateLocalEpoch(long tenantId, String namespace, long epoch);
}
