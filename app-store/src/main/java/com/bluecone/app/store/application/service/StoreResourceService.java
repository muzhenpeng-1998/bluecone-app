package com.bluecone.app.store.application.service;

import com.bluecone.app.resource.api.ResourceClient;
import com.bluecone.app.resource.api.dto.ResourceHandle;
import com.bluecone.app.resource.api.dto.ResourceUploadRequest;
import com.bluecone.app.resource.api.dto.UploadPolicyView;
import com.bluecone.app.resource.api.enums.ResourceOwnerType;
import com.bluecone.app.resource.api.enums.ResourcePurpose;
import com.bluecone.app.resource.api.enums.ResourceProfileCode;
import com.bluecone.app.resource.api.exception.ResourceAccessDeniedException;
import com.bluecone.app.resource.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 将资源中心 ResourceClient 的能力封装为面向门店的服务。
 */
@Service
@RequiredArgsConstructor
public class StoreResourceService {

    private static final Logger log = LoggerFactory.getLogger(StoreResourceService.class);

    private final ResourceClient resourceClient;

    public UploadPolicyView requestStoreLogoUpload(Long storeId,
                                                   String fileName,
                                                   String contentType,
                                                   long sizeBytes,
                                                   String hashSha256) {
        ResourceUploadRequest request = new ResourceUploadRequest(
                ResourceProfileCode.STORE_LOGO,
                ResourceOwnerType.STORE,
                storeId,
                fileName,
                contentType,
                sizeBytes,
                hashSha256);
        return resourceClient.requestUploadPolicy(request);
    }

    public ResourceHandle completeStoreLogoUpload(String uploadToken,
                                                  String storageKey,
                                                  long sizeBytes,
                                                  String hashSha256) {
        return resourceClient.completeUpload(uploadToken, storageKey, sizeBytes, hashSha256);
    }

    public String resolveStoreLogoUrl(Long storeId) {
        try {
            ResourceHandle handle = resourceClient.getMainResource(ResourceOwnerType.STORE, storeId, ResourcePurpose.MAIN_LOGO);
            return handle != null ? handle.url() : null;
        } catch (ResourceNotFoundException ex) {
            log.debug("门店 {} 未绑定 Logo", storeId);
            return null;
        } catch (ResourceAccessDeniedException ex) {
            log.warn("门店 {} 无权访问 Logo", storeId);
            return null;
        }
    }
}
