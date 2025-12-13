package com.bluecone.app.core.contextkit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * ContextMiddlewareKit 全局配置。
 */
@Component
@ConfigurationProperties(prefix = "bluecone.contextkit")
public class ContextKitProperties {

    private Duration l1Ttl = Duration.ofMinutes(5);
    private Duration negativeTtl = Duration.ofSeconds(30);
    private boolean l2Enabled = false;
    private Duration l2Ttl = Duration.ofMinutes(30);
    private Duration versionCheckWindow = Duration.ofSeconds(2);
    private double versionCheckSampleRate = 0.1d;

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
}

