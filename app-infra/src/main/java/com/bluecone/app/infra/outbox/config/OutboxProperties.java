// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/config/OutboxProperties.java
package com.bluecone.app.infra.outbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Outbox 配置属性。
 */
@ConfigurationProperties(prefix = "bluecone.outbox")
public class OutboxProperties {

    private boolean enabled = true;

    private int maxRetries = 5;

    private long initialBackoffMillis = 1000L;

    private double backoffMultiplier = 2.0;

    private int dispatchBatchSize = 100;

    private int cleanRetentionDays = 7;

    private String publishCron = "0/10 * * * * ?";

    private String cleanCron = "0 0 3 * * ?";

    /**
     * 重试策略：基础延迟（秒）。
     */
    private long baseDelaySeconds = 1;

    /**
     * 重试策略：最大延迟（秒）。
     */
    private long maxDelaySeconds = 300;

    /**
     * 重试策略：最大重试次数。
     */
    private int maxRetryCount = 5;

    /**
     * 幂等消费去重 TTL（天）。
     */
    private int consumptionDedupDays = 7;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(final int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getInitialBackoffMillis() {
        return initialBackoffMillis;
    }

    public void setInitialBackoffMillis(final long initialBackoffMillis) {
        this.initialBackoffMillis = initialBackoffMillis;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public void setBackoffMultiplier(final double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }

    public int getDispatchBatchSize() {
        return dispatchBatchSize;
    }

    public void setDispatchBatchSize(final int dispatchBatchSize) {
        this.dispatchBatchSize = dispatchBatchSize;
    }

    public int getCleanRetentionDays() {
        return cleanRetentionDays;
    }

    public void setCleanRetentionDays(final int cleanRetentionDays) {
        this.cleanRetentionDays = cleanRetentionDays;
    }

    public String getPublishCron() {
        return publishCron;
    }

    public void setPublishCron(final String publishCron) {
        this.publishCron = publishCron;
    }

    public String getCleanCron() {
        return cleanCron;
    }

    public void setCleanCron(final String cleanCron) {
        this.cleanCron = cleanCron;
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

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(final int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public int getConsumptionDedupDays() {
        return consumptionDedupDays;
    }

    public void setConsumptionDedupDays(final int consumptionDedupDays) {
        this.consumptionDedupDays = consumptionDedupDays;
    }
}
