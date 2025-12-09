package com.bluecone.app.infra.storage.aliyun;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.bluecone.app.infra.storage.AccessLevel;
import com.bluecone.app.infra.storage.GenerateDownloadUrlRequest;
import com.bluecone.app.infra.storage.GenerateUploadPolicyRequest;
import com.bluecone.app.infra.storage.StorageClient;
import com.bluecone.app.infra.storage.StorageUploadPolicy;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 阿里云 OSS 实现。
 */
public class AliyunOssStorageClient implements StorageClient {

    private final OSS ossClient;
    private final AliyunOssProperties properties;

    public AliyunOssStorageClient(final OSS ossClient, final AliyunOssProperties properties) {
        this.ossClient = Objects.requireNonNull(ossClient, "ossClient must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public StorageUploadPolicy generateUploadPolicy(final GenerateUploadPolicyRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        final String bucket = resolveBucket(request.getBucketName());
        final String storageKey = Objects.requireNonNull(request.getStorageKey(), "storageKey must not be null");
        final long expireSeconds = resolveExpireSeconds(request.getExpireSeconds());

        Date expiration = Date.from(Instant.now().plusSeconds(expireSeconds));
        GeneratePresignedUrlRequest presign = new GeneratePresignedUrlRequest(bucket, storageKey, HttpMethod.PUT);
        presign.setExpiration(expiration);
        Map<String, String> headers = new LinkedHashMap<>();
        if (request.getContentType() != null) {
            presign.setContentType(request.getContentType());
            headers.put("Content-Type", request.getContentType());
        }

        URL url = ossClient.generatePresignedUrl(presign);

        StorageUploadPolicy policy = new StorageUploadPolicy();
        policy.setUploadUrl(url.toString());
        policy.setHttpMethod("PUT");
        policy.setExpiresAt(expiration.toInstant());
        policy.setHeaders(headers);
        policy.setFormFields(Map.of());
        return policy;
    }

    @Override
    public String generateDownloadUrl(final GenerateDownloadUrlRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        final String bucket = resolveBucket(request.getBucketName());
        final String storageKey = Objects.requireNonNull(request.getStorageKey(), "storageKey must not be null");
        final AccessLevel level = request.getAccessLevel() == null ? AccessLevel.PRIVATE : request.getAccessLevel();

        if (level == AccessLevel.PUBLIC_READ) {
            return buildPublicUrl(bucket, storageKey);
        }

        long expireSeconds = resolveExpireSeconds(request.getExpireSeconds());
        Date expiration = new Date(System.currentTimeMillis() + Duration.ofSeconds(expireSeconds).toMillis());
        GeneratePresignedUrlRequest presign = new GeneratePresignedUrlRequest(bucket, storageKey, HttpMethod.GET);
        presign.setExpiration(expiration);
        URL url = ossClient.generatePresignedUrl(presign);
        return url.toString();
    }

    @Override
    public void deleteObject(final String bucketName, final String storageKey) {
        final String bucket = resolveBucket(bucketName);
        Objects.requireNonNull(storageKey, "storageKey must not be null");
        ossClient.deleteObject(bucket, storageKey);
    }

    private String resolveBucket(final String bucketFromRequest) {
        if (bucketFromRequest != null && !bucketFromRequest.isBlank()) {
            return bucketFromRequest;
        }
        if (properties.getDefaultBucket() == null || properties.getDefaultBucket().isBlank()) {
            throw new IllegalArgumentException("bucketName must not be empty");
        }
        return properties.getDefaultBucket();
    }

    private long resolveExpireSeconds(final Long overrideSeconds) {
        if (overrideSeconds != null && overrideSeconds > 0) {
            return overrideSeconds;
        }
        return properties.getDefaultExpireSeconds();
    }

    private String buildPublicUrl(final String bucket, final String storageKey) {
        URI endpointUri = URI.create(properties.getEndpoint());
        String host = endpointUri.getHost();
        String scheme = endpointUri.getScheme() == null ? "https" : endpointUri.getScheme();
        String basePath = storageKey.startsWith("/") ? storageKey.substring(1) : storageKey;
        return scheme + "://" + bucket + "." + host + "/" + basePath;
    }
}

