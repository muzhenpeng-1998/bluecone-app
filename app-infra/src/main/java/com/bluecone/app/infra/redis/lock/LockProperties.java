package com.bluecone.app.infra.redis.lock;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 分布式锁全局配置，通过 application.yml 进行开关与默认时间参数调整。
 */
@ConfigurationProperties(prefix = "bluecone.lock")
public class LockProperties {

    /**
     * 是否启用分布式锁 AOP 能力。
     */
    private boolean enabled = true;

    /**
     * 默认等待时长（毫秒），当注解未显式指定时使用。
     */
    private long defaultWaitTimeMs = 100;

    /**
     * 默认租约时长（毫秒），避免忘记设置导致长时间占锁。
     */
    private long defaultLeaseTimeMs = 3000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getDefaultWaitTimeMs() {
        return defaultWaitTimeMs;
    }

    public void setDefaultWaitTimeMs(long defaultWaitTimeMs) {
        this.defaultWaitTimeMs = defaultWaitTimeMs;
    }

    public long getDefaultLeaseTimeMs() {
        return defaultLeaseTimeMs;
    }

    public void setDefaultLeaseTimeMs(long defaultLeaseTimeMs) {
        this.defaultLeaseTimeMs = defaultLeaseTimeMs;
    }
}
