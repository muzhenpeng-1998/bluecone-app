package com.bluecone.app.infra.storage;

/**
 * 生成下载地址请求。
 */
public class GenerateDownloadUrlRequest {

    private String bucketName;
    private String storageKey;
    private AccessLevel accessLevel = AccessLevel.PRIVATE;
    private Long expireSeconds;

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

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(final AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    public Long getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(final Long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }
}

