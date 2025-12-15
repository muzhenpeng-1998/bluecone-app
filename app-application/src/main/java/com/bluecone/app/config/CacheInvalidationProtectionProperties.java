package com.bluecone.app.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Protection settings for cache invalidation storm handling.
 */
@Component
@ConfigurationProperties(prefix = "bluecone.cache.invalidation.protection")
public class CacheInvalidationProtectionProperties {

    /**
     * Global toggle for protection (storm guard + coalescer + epoch-based bump).
     */
    private boolean enabled = false;

    /**
     * Per-tenant+namespace threshold where we start coalescing events.
     */
    private int coalesceThresholdPerMinute = 60;

    /**
     * Per-tenant+namespace threshold where we enter storm mode and switch to epoch bump.
     */
    private int stormThresholdPerMinute = 300;

    /**
     * Cooldown window once storm mode is entered.
     */
    private Duration stormCooldown = Duration.ofMinutes(2);

    /**
     * Debounce window for coalescer.
     */
    private Duration debounceWindow = Duration.ofMillis(500);

    /**
     * Max keys per single event before we force epoch bump.
     */
    private int maxKeysPerEvent = 50;

    /**
     * Max keys per coalesced batch; when exceeded, we fallback to epoch bump.
     */
    private int maxKeysPerBatch = 200;

    /**
     * Whether epoch-based keying is enabled; kept for documentation, epoch is always used in keys.
     */
    private boolean epochEnabled = true;

    /**
     * L1 epoch cache TTL.
     */
    private Duration epochL1Ttl = Duration.ofSeconds(3);

    /**
     * Whether Redis should be used for storm counters when available.
     */
    private boolean redisStormEnabled = true;

    /**
     * Whether Redis should be used for epoch storage when available.
     */
    private boolean redisEpochEnabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCoalesceThresholdPerMinute() {
        return coalesceThresholdPerMinute;
    }

    public void setCoalesceThresholdPerMinute(int coalesceThresholdPerMinute) {
        this.coalesceThresholdPerMinute = coalesceThresholdPerMinute;
    }

    public int getStormThresholdPerMinute() {
        return stormThresholdPerMinute;
    }

    public void setStormThresholdPerMinute(int stormThresholdPerMinute) {
        this.stormThresholdPerMinute = stormThresholdPerMinute;
    }

    public Duration getStormCooldown() {
        return stormCooldown;
    }

    public void setStormCooldown(Duration stormCooldown) {
        this.stormCooldown = stormCooldown;
    }

    public Duration getDebounceWindow() {
        return debounceWindow;
    }

    public void setDebounceWindow(Duration debounceWindow) {
        this.debounceWindow = debounceWindow;
    }

    public int getMaxKeysPerEvent() {
        return maxKeysPerEvent;
    }

    public void setMaxKeysPerEvent(int maxKeysPerEvent) {
        this.maxKeysPerEvent = maxKeysPerEvent;
    }

    public int getMaxKeysPerBatch() {
        return maxKeysPerBatch;
    }

    public void setMaxKeysPerBatch(int maxKeysPerBatch) {
        this.maxKeysPerBatch = maxKeysPerBatch;
    }

    public boolean isEpochEnabled() {
        return epochEnabled;
    }

    public void setEpochEnabled(boolean epochEnabled) {
        this.epochEnabled = epochEnabled;
    }

    public Duration getEpochL1Ttl() {
        return epochL1Ttl;
    }

    public void setEpochL1Ttl(Duration epochL1Ttl) {
        this.epochL1Ttl = epochL1Ttl;
    }

    public boolean isRedisStormEnabled() {
        return redisStormEnabled;
    }

    public void setRedisStormEnabled(boolean redisStormEnabled) {
        this.redisStormEnabled = redisStormEnabled;
    }

    public boolean isRedisEpochEnabled() {
        return redisEpochEnabled;
    }

    public void setRedisEpochEnabled(boolean redisEpochEnabled) {
        this.redisEpochEnabled = redisEpochEnabled;
    }
}

