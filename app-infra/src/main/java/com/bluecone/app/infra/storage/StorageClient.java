package com.bluecone.app.infra.storage;

/**
 * 统一存储抽象，屏蔽具体存储 SDK。
 */
public interface StorageClient {

    /**
     * 生成直传策略（预签名上传）。
     */
    StorageUploadPolicy generateUploadPolicy(GenerateUploadPolicyRequest request);

    /**
     * 生成下载 URL（公共或带签名）。
     */
    String generateDownloadUrl(GenerateDownloadUrlRequest request);

    /**
     * 删除对象。
     */
    void deleteObject(String bucketName, String storageKey);
}

