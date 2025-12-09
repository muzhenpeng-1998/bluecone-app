package com.bluecone.app.infra.storage.aliyun;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 阿里云 OSS 配置。
 */
@ConfigurationProperties(prefix = "bluecone.storage.aliyun")
public class AliyunOssProperties {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String defaultBucket;
    private long defaultExpireSeconds = 600;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(final String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(final String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(final String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getDefaultBucket() {
        return defaultBucket;
    }

    public void setDefaultBucket(final String defaultBucket) {
        this.defaultBucket = defaultBucket;
    }

    public long getDefaultExpireSeconds() {
        return defaultExpireSeconds;
    }

    public void setDefaultExpireSeconds(final long defaultExpireSeconds) {
        this.defaultExpireSeconds = defaultExpireSeconds;
    }
}

