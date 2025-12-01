package com.bluecone.app.infra.notify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 通知平台基础配置（配置层）。
 *
 * <p>用于控制启停、默认限流/幂等窗口等，支持通过 ConfigCenter 扩展。</p>
 */
@ConfigurationProperties(prefix = "bluecone.notify")
public class NotifyProperties {

    /**
     * 全局开关，关闭后直接拒绝业务请求。
     */
    private boolean enabled = true;

    /**
     * 调试模式：仅记录日志，不真实下发。
     */
    private boolean debugMode = false;

    /**
     * 默认每分钟最大通知条数（租户+场景+通道维度）。
     */
    private int defaultMaxPerMinute = 120;

    /**
     * 幂等窗口（分钟），同一 idempotentKey 在窗口内仅接受一次。
     */
    private int defaultIdempotentMinutes = 5;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public int getDefaultMaxPerMinute() {
        return defaultMaxPerMinute;
    }

    public void setDefaultMaxPerMinute(int defaultMaxPerMinute) {
        this.defaultMaxPerMinute = defaultMaxPerMinute;
    }

    public int getDefaultIdempotentMinutes() {
        return defaultIdempotentMinutes;
    }

    public void setDefaultIdempotentMinutes(int defaultIdempotentMinutes) {
        this.defaultIdempotentMinutes = defaultIdempotentMinutes;
    }
}
