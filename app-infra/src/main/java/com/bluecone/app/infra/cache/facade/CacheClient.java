package com.bluecone.app.infra.cache.facade;

import com.bluecone.app.infra.cache.core.CacheEngine;
import com.bluecone.app.infra.cache.core.CacheKey;
import com.bluecone.app.infra.cache.profile.CacheProfile;
import com.bluecone.app.infra.cache.profile.CacheProfileName;
import com.bluecone.app.infra.cache.profile.CacheProfileRegistry;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 门面：业务只关心 Profile + KeyBuilder，不需要感知多级存储细节。
 */
public class CacheClient {

    private final CacheEngine cacheEngine;
    private final CacheProfileRegistry profileRegistry;

    public CacheClient(CacheEngine cacheEngine, CacheProfileRegistry profileRegistry) {
        this.cacheEngine = cacheEngine;
        this.profileRegistry = profileRegistry;
    }

    public <T> T get(CacheProfileName profileName,
                     Function<Object, CacheKey> keyBuilder,
                     Object bizId,
                     Class<T> type,
                     Supplier<T> loader) {
        CacheProfile profile = profileRegistry.getProfile(Objects.requireNonNull(profileName));
        CacheKey key = keyBuilder.apply(bizId);
        return cacheEngine.get(key, type, loader, profile);
    }

    public void evict(CacheProfileName profileName, CacheKey key) {
        CacheProfile profile = profileRegistry.getProfile(Objects.requireNonNull(profileName));
        cacheEngine.evict(key, profile);
    }
}
