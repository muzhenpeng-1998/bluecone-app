package com.bluecone.app.infra.cache.consistency;

import com.bluecone.app.infra.cache.core.CacheKey;

import java.util.function.Consumer;

/**
 * 一致性总线抽象，屏蔽 pub/sub 细节。
 */
public interface ConsistencyBus {

    void publishInvalidation(CacheKey key, String reason);

    void registerInvalidationListener(Consumer<CacheKey> listener);
}
