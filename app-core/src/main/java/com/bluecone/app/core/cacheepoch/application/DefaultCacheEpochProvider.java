package com.bluecone.app.core.cacheepoch.application;

import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Default epoch provider with mandatory Caffeine L1 cache and optional Redis L2.
 *
 * <p>When Redis is available, it is treated as the source of truth and epoch
 * values are kept globally monotonic via Redis INCR. Without Redis, a local
 * {@link AtomicLong} per (tenant, namespace) is used, which only guarantees
 * monotonicity within a single JVM instance.</p>
 */
public class DefaultCacheEpochProvider implements CacheEpochProvider {

    private static final long INITIAL_EPOCH = 1L;

    private final Cache<String, Long> l1;
    private final StringRedisTemplate redisTemplate;
    private final boolean redisEnabled;
    private final Duration redisTtl;

    private final ConcurrentMap<String, AtomicLong> localEpochs = new ConcurrentHashMap<>();

    /**
     * Create a provider without Redis (single-instance monotonicity only).
     *
     * @param l1Ttl TTL for L1 Caffeine entries
     */
    public DefaultCacheEpochProvider(Duration l1Ttl) {
        this(l1Ttl, null, false, null);
    }

    /**
     * Create a provider with optional Redis.
     *
     * @param l1Ttl       TTL for L1 Caffeine entries
     * @param redisTemplate Redis template, may be {@code null}
     * @param redisEnabled  whether Redis should be used when available
     * @param redisTtl      TTL for Redis keys, may be {@code null} for no explicit TTL
     */
    public DefaultCacheEpochProvider(Duration l1Ttl,
                                     StringRedisTemplate redisTemplate,
                                     boolean redisEnabled,
                                     Duration redisTtl) {
        Objects.requireNonNull(l1Ttl, "l1Ttl");
        this.l1 = Caffeine.newBuilder()
                .expireAfterWrite(l1Ttl.toMillis(), TimeUnit.MILLISECONDS)
                .maximumSize(10_000)
                .build();
        this.redisTemplate = redisTemplate;
        this.redisEnabled = redisEnabled && redisTemplate != null;
        this.redisTtl = redisTtl;
    }

    @Override
    public long currentEpoch(long tenantId, String namespace) {
        String key = buildKey(tenantId, namespace);
        Long cached = l1.getIfPresent(key);
        if (cached != null && cached > 0L) {
            return cached;
        }

        long epoch;
        if (redisEnabled) {
            epoch = readOrInitFromRedis(key);
        } else {
            epoch = localEpochs
                    .computeIfAbsent(key, k -> new AtomicLong(INITIAL_EPOCH))
                    .get();
        }
        l1.put(key, epoch);
        return epoch;
    }

    @Override
    public long bumpEpoch(long tenantId, String namespace) {
        String key = buildKey(tenantId, namespace);
        long epoch;
        if (redisEnabled) {
            epoch = bumpInRedis(key);
        } else {
            AtomicLong counter = localEpochs.computeIfAbsent(key, k -> new AtomicLong(INITIAL_EPOCH));
            epoch = counter.incrementAndGet();
        }
        l1.put(key, epoch);
        return epoch;
    }

    @Override
    public void updateLocalEpoch(long tenantId, String namespace, long epoch) {
        if (epoch <= 0L) {
            return;
        }
        String key = buildKey(tenantId, namespace);
        l1.put(key, epoch);
    }

    private String buildKey(long tenantId, String namespace) {
        return tenantId + ":" + Objects.requireNonNull(namespace, "namespace");
    }

    private String redisKey(String compositeKey) {
        return "bc:epoch:" + compositeKey;
    }

    private long readOrInitFromRedis(String compositeKey) {
        String rKey = redisKey(compositeKey);
        try {
            String val = redisTemplate.opsForValue().get(rKey);
            if (val != null) {
                trySetRedisTtl(rKey);
                return parseEpoch(val);
            }
            Boolean success = redisTemplate.opsForValue().setIfAbsent(rKey, Long.toString(INITIAL_EPOCH));
            long epoch = INITIAL_EPOCH;
            if (Boolean.FALSE.equals(success)) {
                String v2 = redisTemplate.opsForValue().get(rKey);
                if (v2 != null) {
                    epoch = parseEpoch(v2);
                }
            }
            trySetRedisTtl(rKey);
            return epoch;
        } catch (DataAccessException ex) {
            // Redis unavailable, fall back to local
            return localEpochs
                    .computeIfAbsent(compositeKey, k -> new AtomicLong(INITIAL_EPOCH))
                    .get();
        }
    }

    private long bumpInRedis(String compositeKey) {
        String rKey = redisKey(compositeKey);
        try {
            Long v = redisTemplate.opsForValue().increment(rKey);
            long epoch = v != null && v > 0L ? v : INITIAL_EPOCH;
            trySetRedisTtl(rKey);
            return epoch;
        } catch (DataAccessException ex) {
            // Redis unavailable, fall back to local
            AtomicLong counter = localEpochs.computeIfAbsent(compositeKey, k -> new AtomicLong(INITIAL_EPOCH));
            return counter.incrementAndGet();
        }
    }

    private void trySetRedisTtl(String rKey) {
        if (redisTtl == null || redisTtl.isZero() || redisTtl.isNegative()) {
            return;
        }
        redisTemplate.expire(rKey, redisTtl);
    }

    private long parseEpoch(String val) {
        try {
            long parsed = Long.parseLong(val);
            return parsed > 0L ? parsed : INITIAL_EPOCH;
        } catch (NumberFormatException ex) {
            return INITIAL_EPOCH;
        }
    }
}
