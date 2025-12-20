package com.bluecone.app.application.resource;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.resource.api.ResourceClient;
import com.bluecone.app.resource.api.dto.ResourceHandle;
import com.bluecone.app.resource.api.dto.ResourceUploadRequest;
import com.bluecone.app.resource.api.dto.UploadPolicyView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资源中心对外 HTTP 接口，供前端/业务模块获取上传策略并确认资源落库。
 */
@RestController
@RequestMapping("/api/resource")
public class ResourceController {

    private static final Logger log = LoggerFactory.getLogger(ResourceController.class);

    private final ResourceClient resourceClient;

    public ResourceController(ResourceClient resourceClient) {
        this.resourceClient = resourceClient;
    }

    @PostMapping("/upload/policy")
    public ApiResponse<UploadPolicyView> requestUploadPolicy(@RequestBody ResourceUploadRequest request) {
        log.info("请求资源上传策略 profile={} owner={}/{}", request.profileCode(), request.ownerType(), request.ownerId());
        UploadPolicyView policy = resourceClient.requestUploadPolicy(request);
        return ApiResponse.success(policy);
    }

    @PostMapping("/upload/complete")
    public ApiResponse<ResourceHandle> completeUpload(@RequestBody ResourceUploadCompleteRequest request) {
        log.info("上传完成回调 uploadToken={}", request.uploadToken());
        ResourceHandle handle = resourceClient.completeUpload(
                request.uploadToken(),
                request.storageKey(),
                request.sizeBytes(),
                request.hashSha256());
        return ApiResponse.success(handle);
    }

    public static record ResourceUploadCompleteRequest(String uploadToken,
                                                       String storageKey,
                                                       long sizeBytes,
                                                       String hashSha256) {
    }
}
