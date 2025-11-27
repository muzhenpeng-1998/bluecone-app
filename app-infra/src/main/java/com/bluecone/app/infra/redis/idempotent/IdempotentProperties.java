package com.bluecone.app.infra.redis.idempotent;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 幂等能力的全局配置项。
 */
@ConfigurationProperties(prefix = "bluecone.idempotent")
public class IdempotentProperties {

    /**
     * 是否启用幂等切面。
     */
    private boolean enabled = true;

    /**
     * 幂等键的默认过期时间（秒）。
     */
    private int defaultExpireSeconds = 300;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDefaultExpireSeconds() {
        return defaultExpireSeconds;
    }

    public void setDefaultExpireSeconds(int defaultExpireSeconds) {
        this.defaultExpireSeconds = defaultExpireSeconds;
    }
}
