package com.bluecone.app.core.cacheinval.guard;

import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Default implementation of {@link InvalidationStormGuard} with optional Redis.
 *
 * <p>Counts events per tenant+namespace in 1 minute windows. When the count
 * exceeds {@code coalesceThresholdPerMinute}, the decision switches to
 * {@link InvalidationDecision#COALESCE}. When the count exceeds
 * {@code stormThresholdPerMinute} or storm mode is active, the decision
 * becomes {@link InvalidationDecision#EPOCH_BUMP}. Additionally, if the number
 * of keys in a single event exceeds {@code maxKeysPerEvent}, epoch bump is
 * chosen immediately.</p>
 */
public class DefaultInvalidationStormGuard implements InvalidationStormGuard {

    private final int coalesceThresholdPerMinute;
    private final int stormThresholdPerMinute;
    private final Duration stormCooldown;
    private final int maxKeysPerEvent;
    private final boolean redisEnabled;

    private final StringRedisTemplate redisTemplate;

    private final Cache<String, Long> localCounters;
    private final Cache<String, Instant> localStormMode;

    public DefaultInvalidationStormGuard(int coalesceThresholdPerMinute,
                                         int stormThresholdPerMinute,
                                         Duration stormCooldown,
                                         int maxKeysPerEvent,
                                         StringRedisTemplate redisTemplate,
                                         boolean redisEnabled) {
        this.coalesceThresholdPerMinute = coalesceThresholdPerMinute;
        this.stormThresholdPerMinute = stormThresholdPerMinute;
        this.stormCooldown = Objects.requireNonNull(stormCooldown, "stormCooldown");
        this.maxKeysPerEvent = maxKeysPerEvent;
        this.redisTemplate = redisTemplate;
        this.redisEnabled = redisEnabled && redisTemplate != null;
        this.localCounters = Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
        this.localStormMode = Caffeine.newBuilder()
                .expireAfterWrite(stormCooldown.toMillis(), TimeUnit.MILLISECONDS)
                .maximumSize(10_000)
                .build();
    }

    @Override
    public GuardDecision decide(CacheInvalidationEvent evt) {
        if (evt == null) {
            return new GuardDecision(InvalidationDecision.DIRECT_KEYS, false, "null-event", 0L, 0);
        }
        int keysCount = evt.keys() != null ? evt.keys().size() : 0;
        if (keysCount > maxKeysPerEvent) {
            return new GuardDecision(InvalidationDecision.EPOCH_BUMP, false, "keys-exceed-max", 0L, keysCount);
        }

        String nsKey = buildNamespaceKey(evt);
        long minuteEpoch = currentMinuteEpoch();

        boolean stormMode = isStormMode(nsKey);
        long count = incrementCounter(nsKey, minuteEpoch);

        if (stormMode) {
            return new GuardDecision(InvalidationDecision.EPOCH_BUMP, true, "storm-mode", 0L, keysCount);
        }

        if (count >= stormThresholdPerMinute) {
            enterStormMode(nsKey);
            return new GuardDecision(InvalidationDecision.EPOCH_BUMP, true, "storm-threshold", 0L, keysCount);
        }

        if (count >= coalesceThresholdPerMinute) {
            return new GuardDecision(InvalidationDecision.COALESCE, false, "coalesce-threshold", 0L, keysCount);
        }

        return new GuardDecision(InvalidationDecision.DIRECT_KEYS, false, "direct", 0L, keysCount);
    }

    private String buildNamespaceKey(CacheInvalidationEvent evt) {
        long tenantId = evt.tenantId();
        String namespace = evt.namespace() != null ? evt.namespace() : "UNKNOWN";
        return tenantId + ":" + namespace;
    }

    private long currentMinuteEpoch() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        return now.getEpochSecond() / 60;
    }

    private boolean isStormMode(String nsKey) {
        if (redisEnabled) {
            String modeKey = stormModeRedisKey(nsKey);
            Boolean exists = redisTemplate.hasKey(modeKey);
            return Boolean.TRUE.equals(exists);
        }
        Instant expiresAt = localStormMode.getIfPresent(nsKey);
        return expiresAt != null && expiresAt.isAfter(Instant.now());
    }

    private long incrementCounter(String nsKey, long minuteEpoch) {
        if (redisEnabled) {
            String cntKey = counterRedisKey(nsKey, minuteEpoch);
            Long v = redisTemplate.opsForValue().increment(cntKey);
            if (v != null && v == 1L) {
                redisTemplate.expire(cntKey, Duration.ofSeconds(120));
            }
            return v != null ? v : 0L;
        }
        String localKey = nsKey + ":" + minuteEpoch;
        Long current = localCounters.getIfPresent(localKey);
        long next = current == null ? 1L : current + 1L;
        localCounters.put(localKey, next);
        return next;
    }

    private void enterStormMode(String nsKey) {
        if (redisEnabled) {
            String modeKey = stormModeRedisKey(nsKey);
            redisTemplate.opsForValue().set(modeKey, "1", stormCooldown);
        } else {
            localStormMode.put(nsKey, Instant.now().plus(stormCooldown));
        }
    }

    private String counterRedisKey(String nsKey, long minuteEpoch) {
        return "bc:storm:cnt:" + nsKey + ":" + minuteEpoch;
    }

    private String stormModeRedisKey(String nsKey) {
        return "bc:storm:mode:" + nsKey;
    }
}

