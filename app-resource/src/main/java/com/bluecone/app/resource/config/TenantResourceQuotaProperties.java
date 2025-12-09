package com.bluecone.app.resource.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 资源中心租户配额配置，支持默认值与按租户覆写。
 */
@ConfigurationProperties(prefix = "bluecone.resource.quota")
public class TenantResourceQuotaProperties {

    private int defaultDailyUploadCountLimit = 2000;
    private long defaultDailyUploadBytesLimit = 10L * 1024 * 1024 * 1024;
    private Map<Long, TenantQuotaOverride> tenantOverrides = new LinkedHashMap<>();

    public int getDefaultDailyUploadCountLimit() {
        return defaultDailyUploadCountLimit;
    }

    public void setDefaultDailyUploadCountLimit(int defaultDailyUploadCountLimit) {
        this.defaultDailyUploadCountLimit = defaultDailyUploadCountLimit;
    }

    public long getDefaultDailyUploadBytesLimit() {
        return defaultDailyUploadBytesLimit;
    }

    public void setDefaultDailyUploadBytesLimit(long defaultDailyUploadBytesLimit) {
        this.defaultDailyUploadBytesLimit = defaultDailyUploadBytesLimit;
    }

    public Map<Long, TenantQuotaOverride> getTenantOverrides() {
        return tenantOverrides;
    }

    public void setTenantOverrides(Map<Long, TenantQuotaOverride> tenantOverrides) {
        this.tenantOverrides = tenantOverrides;
    }

    public int getDailyUploadCountLimit(long tenantId) {
        TenantQuotaOverride override = tenantOverrides.get(tenantId);
        if (override != null && override.getDailyUploadCountLimit() != null) {
            return override.getDailyUploadCountLimit();
        }
        return defaultDailyUploadCountLimit;
    }

    public long getDailyUploadBytesLimit(long tenantId) {
        TenantQuotaOverride override = tenantOverrides.get(tenantId);
        if (override != null && override.getDailyUploadBytesLimit() != null) {
            return override.getDailyUploadBytesLimit();
        }
        return defaultDailyUploadBytesLimit;
    }

    public static class TenantQuotaOverride {

        private Integer dailyUploadCountLimit;
        private Long dailyUploadBytesLimit;

        public Integer getDailyUploadCountLimit() {
            return dailyUploadCountLimit;
        }

        public void setDailyUploadCountLimit(Integer dailyUploadCountLimit) {
            this.dailyUploadCountLimit = dailyUploadCountLimit;
        }

        public Long getDailyUploadBytesLimit() {
            return dailyUploadBytesLimit;
        }

        public void setDailyUploadBytesLimit(Long dailyUploadBytesLimit) {
            this.dailyUploadBytesLimit = dailyUploadBytesLimit;
        }
    }
}
