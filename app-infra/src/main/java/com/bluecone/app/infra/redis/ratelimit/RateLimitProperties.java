package com.bluecone.app.infra.redis.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 全局限流配置，支持通过 application.yml 调整默认窗口与阈值。
 */
@ConfigurationProperties(prefix = "bluecone.rate-limit")
public class RateLimitProperties {

    /**
     * 是否启用注解式限流。
     */
    private boolean enabled = true;

    /**
     * 默认阈值，当注解未指定 limit 时使用。
     */
    private int defaultLimit = 100;

    /**
     * 默认窗口时长（秒）。
     */
    private int defaultWindowSeconds = 60;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDefaultLimit() {
        return defaultLimit;
    }

    public void setDefaultLimit(int defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public int getDefaultWindowSeconds() {
        return defaultWindowSeconds;
    }

    public void setDefaultWindowSeconds(int defaultWindowSeconds) {
        this.defaultWindowSeconds = defaultWindowSeconds;
    }
}
