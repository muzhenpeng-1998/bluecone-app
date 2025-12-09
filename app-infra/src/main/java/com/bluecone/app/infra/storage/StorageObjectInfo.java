package com.bluecone.app.infra.storage;

/**
 * 存储对象基础信息。
 */
public class StorageObjectInfo {

    private String bucketName;
    private String storageKey;
    private AccessLevel accessLevel;
    private Long sizeBytes;
    private String contentType;

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

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(final Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }
}

