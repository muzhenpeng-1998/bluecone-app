package com.bluecone.app.infra.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Integration Hub 属性配置。
 */
@ConfigurationProperties(prefix = "bluecone.integration")
public class IntegrationProperties {

    /**
     * 是否启用 Integration Hub。
     */
    private boolean enabled = true;

    /**
     * 扫描待投递任务的批次大小。
     */
    private int dispatchBatchSize = 100;

    /**
     * 缓存订阅配置的过期时间（秒）。
     */
    private long subscriptionCacheTtlSeconds = 60;

    /**
     * 默认最大重试次数（可被订阅覆盖）。
     */
    private int defaultMaxRetry = 5;

    /**
     * 重试基础延迟（秒）。
     */
    private long baseDelaySeconds = 2;

    /**
     * 重试最大延迟（秒）。
     */
    private long maxDelaySeconds = 300;

    /**
     * 调度间隔（毫秒），仅用于简化版 @Scheduled。
     */
    private long dispatchIntervalMs = 5000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public int getDispatchBatchSize() {
        return dispatchBatchSize;
    }

    public void setDispatchBatchSize(final int dispatchBatchSize) {
        this.dispatchBatchSize = dispatchBatchSize;
    }

    public long getSubscriptionCacheTtlSeconds() {
        return subscriptionCacheTtlSeconds;
    }

    public void setSubscriptionCacheTtlSeconds(final long subscriptionCacheTtlSeconds) {
        this.subscriptionCacheTtlSeconds = subscriptionCacheTtlSeconds;
    }

    public int getDefaultMaxRetry() {
        return defaultMaxRetry;
    }

    public void setDefaultMaxRetry(final int defaultMaxRetry) {
        this.defaultMaxRetry = defaultMaxRetry;
    }

    public long getBaseDelaySeconds() {
        return baseDelaySeconds;
    }

    public void setBaseDelaySeconds(final long baseDelaySeconds) {
        this.baseDelaySeconds = baseDelaySeconds;
    }

    public long getMaxDelaySeconds() {
        return maxDelaySeconds;
    }

    public void setMaxDelaySeconds(final long maxDelaySeconds) {
        this.maxDelaySeconds = maxDelaySeconds;
    }

    public long getDispatchIntervalMs() {
        return dispatchIntervalMs;
    }

    public void setDispatchIntervalMs(final long dispatchIntervalMs) {
        this.dispatchIntervalMs = dispatchIntervalMs;
    }
}
