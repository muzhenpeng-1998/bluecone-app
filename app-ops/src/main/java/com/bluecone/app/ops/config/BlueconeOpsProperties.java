package com.bluecone.app.ops.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the minimal ops console.
 */
@ConfigurationProperties(prefix = "bluecone.ops.console")
public class BlueconeOpsProperties {

    /**
     * Whether ops console endpoints are enabled. Default: disabled.
     */
    private boolean enabled = false;

    /**
     * Shared secret token required to access /ops endpoints.
     */
    private String token = "";

    /**
     * Whether localhost (127.0.0.1 / ::1) can bypass token checks.
     */
    private boolean allowLocalhost = true;

    /**
     * Whether to allow token from query parameter ?token=xxx in addition to headers.
     * Default is false for safety.
     */
    private boolean allowQueryToken = false;

    /**
     * TTL for cached summary results.
     */
    private Duration cacheTtl = Duration.ofSeconds(2);

    /**
     * Maximum page size for drill-down APIs.
     */
    private int maxPageSize = 100;

    /**
     * Whether payload/body fields are allowed to be exposed via drill-down APIs.
     * Default is false for safety.
     */
    private boolean exposePayload = false;

    /**
     * Maximum length for error messages in drill-down responses.
     */
    private int maxErrorMsgLen = 200;

    /**
     * Maximum length for payload fields when exposePayload is enabled.
     */
    private int maxPayloadLen = 2000;

    /**
     * Cache TTL for drill-down APIs to reduce repetitive DB queries.
     */
    private Duration drillCacheTtl = Duration.ofSeconds(1);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isAllowLocalhost() {
        return allowLocalhost;
    }

    public void setAllowLocalhost(boolean allowLocalhost) {
        this.allowLocalhost = allowLocalhost;
    }

    public boolean isAllowQueryToken() {
        return allowQueryToken;
    }

    public void setAllowQueryToken(boolean allowQueryToken) {
        this.allowQueryToken = allowQueryToken;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public boolean isExposePayload() {
        return exposePayload;
    }

    public void setExposePayload(boolean exposePayload) {
        this.exposePayload = exposePayload;
    }

    public int getMaxErrorMsgLen() {
        return maxErrorMsgLen;
    }

    public void setMaxErrorMsgLen(int maxErrorMsgLen) {
        this.maxErrorMsgLen = maxErrorMsgLen;
    }

    public int getMaxPayloadLen() {
        return maxPayloadLen;
    }

    public void setMaxPayloadLen(int maxPayloadLen) {
        this.maxPayloadLen = maxPayloadLen;
    }

    public Duration getDrillCacheTtl() {
        return drillCacheTtl;
    }

    public void setDrillCacheTtl(Duration drillCacheTtl) {
        this.drillCacheTtl = drillCacheTtl;
    }
}
