package com.bluecone.app.config;

import com.bluecone.app.core.cacheinval.transport.InvalidationTransport;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuration for event-driven cache invalidation.
 */
@Component
@ConfigurationProperties(prefix = "bluecone.cache.invalidation")
public class CacheInvalidationProperties {

    /**
     * Global toggle for cache invalidation events.
     */
    private boolean enabled = false;

    /**
     * Transport selection. AUTO prefers OUTBOX when available, otherwise REDIS_PUBSUB,
     * and falls back to INPROCESS.
     */
    private InvalidationTransport transport = InvalidationTransport.OUTBOX;

    /**
     * Redis topic name when using REDIS_PUBSUB transport.
     */
    private String redisTopic = "bc:cache:inval";

    /**
     * TTL for recently seen event IDs when doing local de-duplication.
     */
    private Duration recentEventTtl = Duration.ofMinutes(1);

    /**
     * Maximum number of keys allowed per event to guard against very large invalidations.
     */
    private int maxKeysPerEvent = 50;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public InvalidationTransport getTransport() {
        return transport;
    }

    public void setTransport(InvalidationTransport transport) {
        this.transport = transport;
    }

    public String getRedisTopic() {
        return redisTopic;
    }

    public void setRedisTopic(String redisTopic) {
        this.redisTopic = redisTopic;
    }

    public Duration getRecentEventTtl() {
        return recentEventTtl;
    }

    public void setRecentEventTtl(Duration recentEventTtl) {
        this.recentEventTtl = recentEventTtl;
    }

    public int getMaxKeysPerEvent() {
        return maxKeysPerEvent;
    }

    public void setMaxKeysPerEvent(int maxKeysPerEvent) {
        this.maxKeysPerEvent = maxKeysPerEvent;
    }
}

