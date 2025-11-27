package com.bluecone.app.infra.cache.core;

import com.bluecone.app.infra.cache.consistency.ConsistencyBus;
import com.bluecone.app.infra.cache.profile.CacheProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 多级缓存内核：聚合 L1/L2 + 一致性总线，统一调度读取、回写和失效。
 */
public class CacheEngine {

    private static final Logger log = LoggerFactory.getLogger(CacheEngine.class);

    private final CacheStore l1;
    private final CacheStore l2;
    private final ConsistencyBus consistencyBus;

    public CacheEngine(CacheStore l1, CacheStore l2, ConsistencyBus consistencyBus) {
        this.l1 = l1;
        this.l2 = l2;
        this.consistencyBus = consistencyBus;
        this.consistencyBus.registerInvalidationListener(this::handleRemoteInvalidation);
    }

    public <T> T get(CacheKey key, Class<T> type, Supplier<T> loader, CacheProfile profile) {
        Optional<T> l1Value = l1.get(key, type);
        if (l1Value.isPresent()) {
            log.debug("cache.l1.hit key={} profile={}", key, profile.name());
            return unwrap(l1Value.get());
        }

        Optional<T> l2Value = l2.get(key, type);
        if (l2Value.isPresent()) {
            log.debug("cache.l2.hit key={} profile={}", key, profile.name());
            Object storeValue = l2Value.get();
            l1.put(key, storeValue, l1Ttl(profile));
            return unwrap(storeValue);
        }

        T loaded = loader.get();
        Object storeValue = toStoreValue(loaded, profile);
        if (storeValue != null) {
            if (profile.strongConsistency()) {
                l2.put(key, storeValue, profile.ttl());
            }
            l1.put(key, storeValue, l1Ttl(profile));
            log.debug("cache.populate key={} profile={} from=loader", key, profile.name());
        }
        return loaded;
    }

    public void put(CacheKey key, Object value, CacheProfile profile) {
        Object storeValue = toStoreValue(value, profile);
        if (storeValue == null) {
            return;
        }
        if (profile.strongConsistency()) {
            l2.put(key, storeValue, profile.ttl());
        }
        l1.put(key, storeValue, l1Ttl(profile));
        log.debug("cache.put key={} profile={}", key, profile.name());
    }

    public void evict(CacheKey key, CacheProfile profile) {
        l1.evict(key);
        l2.evict(key);
        consistencyBus.publishInvalidation(key, "EXPLICIT_EVICT");
        if (profile.strongConsistency()) {
            log.debug("cache.evict key={} profile={} broadcast", key, profile.name());
        } else {
            log.debug("cache.evict key={} profile={}", key, profile.name());
        }
    }

    private Duration l1Ttl(CacheProfile profile) {
        if (profile.hotKey()) {
            return profile.ttl().plus(profile.ttl().dividedBy(2));
        }
        return profile.ttl();
    }

    private Object toStoreValue(Object value, CacheProfile profile) {
        if (value == null) {
            return profile.cacheNull() ? NullValue.INSTANCE : null;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private <T> T unwrap(Object cached) {
        if (cached instanceof NullValue) {
            return null;
        }
        return (T) cached;
    }

    private void handleRemoteInvalidation(CacheKey key) {
        l1.evict(key);
        log.debug("cache.l1.invalidated key={} source=bus", key);
    }
}
