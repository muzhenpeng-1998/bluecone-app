package com.bluecone.app.infra.storage;

/**
 * 生成直传策略请求。
 */
public class GenerateUploadPolicyRequest {

    private String bucketName;
    private String storageKey;
    private String contentType;
    private Long maxSizeBytes;
    private Long expireSeconds;
    private AccessLevel accessLevel = AccessLevel.PRIVATE;

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(final String bucketName) {
        this.bucketName = bucketName;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(final String storageKey) {
        this.storageKey = storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    public Long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(final Long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public Long getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(final Long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(final AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }
}

