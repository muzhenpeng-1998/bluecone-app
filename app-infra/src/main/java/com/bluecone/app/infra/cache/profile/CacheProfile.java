package com.bluecone.app.infra.cache.profile;

import java.time.Duration;
import java.util.Objects;

/**
 * CacheProfile 将 TTL / 一致性 / 热点策略集中管理，避免分散注解属性。
 */
public final class CacheProfile {

    private final CacheProfileName name;
    private final String domain;
    private final Duration ttl;
    private final boolean cacheNull;
    private final boolean strongConsistency;
    private final boolean hotKey;

    private CacheProfile(Builder builder) {
        this.name = Objects.requireNonNull(builder.name);
        this.domain = Objects.requireNonNull(builder.domain);
        this.ttl = Objects.requireNonNull(builder.ttl);
        this.cacheNull = builder.cacheNull;
        this.strongConsistency = builder.strongConsistency;
        this.hotKey = builder.hotKey;
    }

    public static Builder builder(CacheProfileName name) {
        return new Builder(name);
    }

    public CacheProfileName name() {
        return name;
    }

    public String domain() {
        return domain;
    }

    public Duration ttl() {
        return ttl;
    }

    public boolean cacheNull() {
        return cacheNull;
    }

    public boolean strongConsistency() {
        return strongConsistency;
    }

    public boolean hotKey() {
        return hotKey;
    }

    public static final class Builder {
        private final CacheProfileName name;
        private String domain;
        private Duration ttl = Duration.ofMinutes(5);
        private boolean cacheNull;
        private boolean strongConsistency = true;
        private boolean hotKey;

        private Builder(CacheProfileName name) {
            this.name = name;
            this.domain = name.name().toLowerCase().replace('_', '-');
        }

        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder ttl(Duration ttl) {
            this.ttl = ttl;
            return this;
        }

        public Builder cacheNull(boolean cacheNull) {
            this.cacheNull = cacheNull;
            return this;
        }

        public Builder strongConsistency(boolean strongConsistency) {
            this.strongConsistency = strongConsistency;
            return this;
        }

        public Builder hotKey(boolean hotKey) {
            this.hotKey = hotKey;
            return this;
        }

        public CacheProfile build() {
            return new CacheProfile(this);
        }
    }
}
