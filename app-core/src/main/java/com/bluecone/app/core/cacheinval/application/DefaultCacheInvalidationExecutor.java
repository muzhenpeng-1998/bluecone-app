package com.bluecone.app.core.cacheinval.application;

import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;
import com.bluecone.app.core.contextkit.CacheKey;
import com.bluecone.app.core.contextkit.ContextCache;

import java.util.List;
import java.util.Objects;

/**
 * Default invalidation executor backed by {@link ContextCache}.
 *
 * <p>Each event key is translated into a {@link CacheKey} using the provided
 * namespace, and {@link ContextCache#invalidate(CacheKey)} is called. If the
 * cache is a {@code TwoLevelContextCache}, both L1 and L2 are invalidated.</p>
 */
public class DefaultCacheInvalidationExecutor implements CacheInvalidationExecutor {

    private final ContextCache cache;
    private final CacheEpochProvider epochProvider;

    public DefaultCacheInvalidationExecutor(ContextCache cache) {
        this(cache, null);
    }

    public DefaultCacheInvalidationExecutor(ContextCache cache, CacheEpochProvider epochProvider) {
        this.cache = Objects.requireNonNull(cache, "cache");
        this.epochProvider = epochProvider;
    }

    @Override
    public void execute(CacheInvalidationEvent event) {
        if (event == null) {
            return;
        }
        List<String> keys = event.keys();
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            if (key == null || key.isEmpty()) {
                continue;
            }
            String keySuffix = key;
            if (epochProvider != null && !event.epochBump()) {
                keySuffix = buildEpochKey(event, key);
            }
            CacheKey cacheKey = new CacheKey(event.namespace(), keySuffix);
            cache.invalidate(cacheKey);
        }
    }

    private String buildEpochKey(CacheInvalidationEvent event, String semanticKey) {
        long tenantId = event.tenantId();
        String namespace = event.namespace();
        long epoch = epochProvider.currentEpoch(tenantId, namespace);

        int idx = semanticKey.indexOf(':');
        String scopeIdPart = idx >= 0 && idx + 1 < semanticKey.length()
                ? semanticKey.substring(idx + 1)
                : semanticKey;
        return tenantId + ":" + epoch + ":" + scopeIdPart;
    }
}
