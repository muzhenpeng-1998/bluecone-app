package com.bluecone.app.resource.infrastructure.storage;

import com.bluecone.app.infra.storage.GenerateDownloadUrlRequest;
import com.bluecone.app.infra.storage.GenerateUploadPolicyRequest;
import com.bluecone.app.infra.storage.StorageClient;
import com.bluecone.app.infra.storage.StorageUploadPolicy;
import org.springframework.stereotype.Component;

/**
 * 封装对 Infra StorageClient 的调用，未来可统一加日志/监控。
 */
@Component
public class StorageClientDelegate {

    private final StorageClient storageClient;

    public StorageClientDelegate(StorageClient storageClient) {
        this.storageClient = storageClient;
    }

    public StorageUploadPolicy generateUploadPolicy(GenerateUploadPolicyRequest request) {
        return storageClient.generateUploadPolicy(request);
    }

    public String generateDownloadUrl(GenerateDownloadUrlRequest request) {
        return storageClient.generateDownloadUrl(request);
    }

    public void deleteObject(String bucketName, String storageKey) {
        storageClient.deleteObject(bucketName, storageKey);
    }
}
