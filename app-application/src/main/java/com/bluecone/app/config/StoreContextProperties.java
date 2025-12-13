package com.bluecone.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 门店上下文中间件配置。
 */
@Component
@ConfigurationProperties(prefix = "bluecone.store.context")
public class StoreContextProperties {

    private boolean enabled = true;
    private List<String> includePaths = List.of("/api/mini/**");
    private List<String> excludePaths = List.of("/ops/**", "/actuator/**", "/api/admin/**");

    private Cache cache = new Cache();

    private Duration versionCheckWindow = Duration.ofSeconds(2);
    private double versionCheckSampleRate = 0.1d;

    private boolean requireStoreId = true;
    private List<String> allowMissingStoreIdPaths = List.of("/api/mini/home/**");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getIncludePaths() {
        return includePaths;
    }

    public void setIncludePaths(List<String> includePaths) {
        this.includePaths = includePaths;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public Duration getVersionCheckWindow() {
        return versionCheckWindow;
    }

    public void setVersionCheckWindow(Duration versionCheckWindow) {
        this.versionCheckWindow = versionCheckWindow;
    }

    public double getVersionCheckSampleRate() {
        return versionCheckSampleRate;
    }

    public void setVersionCheckSampleRate(double versionCheckSampleRate) {
        this.versionCheckSampleRate = versionCheckSampleRate;
    }

    public boolean isRequireStoreId() {
        return requireStoreId;
    }

    public void setRequireStoreId(boolean requireStoreId) {
        this.requireStoreId = requireStoreId;
    }

    public List<String> getAllowMissingStoreIdPaths() {
        return allowMissingStoreIdPaths;
    }

    public void setAllowMissingStoreIdPaths(List<String> allowMissingStoreIdPaths) {
        this.allowMissingStoreIdPaths = allowMissingStoreIdPaths;
    }

    public static class Cache {
        private Duration l1Ttl = Duration.ofMinutes(5);
        private Duration negativeTtl = Duration.ofSeconds(30);
        private boolean l2Enabled = true;
        private Duration l2Ttl = Duration.ofMinutes(30);

        public Duration getL1Ttl() {
            return l1Ttl;
        }

        public void setL1Ttl(Duration l1Ttl) {
            this.l1Ttl = l1Ttl;
        }

        public Duration getNegativeTtl() {
            return negativeTtl;
        }

        public void setNegativeTtl(Duration negativeTtl) {
            this.negativeTtl = negativeTtl;
        }

        public boolean isL2Enabled() {
            return l2Enabled;
        }

        public void setL2Enabled(boolean l2Enabled) {
            this.l2Enabled = l2Enabled;
        }

        public Duration getL2Ttl() {
            return l2Ttl;
        }

        public void setL2Ttl(Duration l2Ttl) {
            this.l2Ttl = l2Ttl;
        }
    }
}

