package com.bluecone.app.resource.support;

import com.bluecone.app.infra.storage.AccessLevel;
import com.bluecone.app.resource.api.enums.ResourceProfileCode;
import com.bluecone.app.resource.api.enums.ResourcePurpose;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 描述单个资源 profile 的配置。
 */
public final class ResourceProfileSpec {

    private final ResourceProfileCode profileCode;
    private final ResourcePurpose purpose;
    private final String bucketName;
    private final String basePath;
    private final AccessLevel accessLevel;
    private final long maxSizeBytes;
    private final Set<String> allowedExtensions;
    private final Set<String> allowedContentTypes;
    private final long expireSeconds;

    public ResourceProfileSpec(ResourceProfileCode profileCode,
                               ResourcePurpose purpose,
                               String bucketName,
                               String basePath,
                               AccessLevel accessLevel,
                               long maxSizeBytes,
                               Set<String> allowedExtensions,
                               Set<String> allowedContentTypes,
                               long expireSeconds) {
        this.profileCode = Objects.requireNonNull(profileCode, "profileCode");
        this.purpose = Objects.requireNonNull(purpose, "purpose");
        this.bucketName = Objects.requireNonNull(bucketName, "bucketName");
        this.basePath = Objects.requireNonNull(basePath, "basePath");
        this.accessLevel = Objects.requireNonNull(accessLevel, "accessLevel");
        this.maxSizeBytes = maxSizeBytes;
        this.allowedExtensions = Collections.unmodifiableSet(new LinkedHashSet<>(allowedExtensions));
        this.allowedContentTypes = Collections.unmodifiableSet(new LinkedHashSet<>(allowedContentTypes));
        this.expireSeconds = expireSeconds;
    }

    public ResourceProfileCode getProfileCode() {
        return profileCode;
    }

    public ResourcePurpose getPurpose() {
        return purpose;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getBasePath() {
        return basePath;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public Set<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public Set<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }
}
