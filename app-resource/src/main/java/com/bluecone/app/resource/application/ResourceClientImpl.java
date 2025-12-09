package com.bluecone.app.resource.application;

import com.bluecone.app.core.id.IdService;
import com.bluecone.app.infra.storage.AccessLevel;
import com.bluecone.app.infra.storage.GenerateDownloadUrlRequest;
import com.bluecone.app.infra.storage.GenerateUploadPolicyRequest;
import com.bluecone.app.infra.storage.StorageProperties;
import com.bluecone.app.infra.storage.StorageUploadPolicy;
import com.bluecone.app.infra.tenant.TenantContext;
import com.bluecone.app.resource.api.ResourceClient;
import com.bluecone.app.resource.api.dto.BindResourceCommand;
import com.bluecone.app.resource.api.dto.ResourceHandle;
import com.bluecone.app.resource.api.dto.ResourceQuery;
import com.bluecone.app.resource.api.dto.ResourceUploadRequest;
import com.bluecone.app.resource.api.dto.UploadPolicyView;
import com.bluecone.app.resource.api.dto.UnbindResourceCommand;
import com.bluecone.app.resource.api.enums.ResourceOwnerType;
import com.bluecone.app.resource.api.enums.ResourcePurpose;
import com.bluecone.app.resource.api.enums.ResourceProfileCode;
import com.bluecone.app.resource.api.exception.ResourceUploadException;
import com.bluecone.app.resource.domain.service.ResourceDomainService;
import com.bluecone.app.resource.domain.service.ResourcePolicyService;
import com.bluecone.app.resource.infrastructure.repository.ResourceBindingDO;
import com.bluecone.app.resource.infrastructure.repository.ResourceBindingMapper;
import com.bluecone.app.resource.infrastructure.repository.ResourceObjectDO;
import com.bluecone.app.resource.infrastructure.repository.ResourceObjectMapper;
import com.bluecone.app.resource.infrastructure.session.UploadSessionStore;
import com.bluecone.app.resource.infrastructure.storage.StorageClientDelegate;
import com.bluecone.app.resource.support.ResourceProfileSpec;
import com.bluecone.app.resource.support.ResourceProfilesLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 资源中心 API 的默认实现，后续将桥接领域与基础设施能力。
 */
public class ResourceClientImpl implements ResourceClient {

    private static final Logger log = LoggerFactory.getLogger(ResourceClientImpl.class);

    private final ResourceDomainService resourceDomainService;
    private final ResourcePolicyService resourcePolicyService;
    private final ResourceObjectMapper resourceObjectMapper;
    private final ResourceBindingMapper resourceBindingMapper;
    private final UploadSessionStore uploadSessionStore;
    private final StorageClientDelegate storageClientDelegate;
    private final ResourceProfilesLoader profilesLoader;
    private final IdService idService;
    private final StorageProperties storageProperties;

    public ResourceClientImpl(ResourceDomainService resourceDomainService,
                              ResourcePolicyService resourcePolicyService,
                              ResourceObjectMapper resourceObjectMapper,
                              ResourceBindingMapper resourceBindingMapper,
                              UploadSessionStore uploadSessionStore,
                              StorageClientDelegate storageClientDelegate,
                              ResourceProfilesLoader profilesLoader,
                              IdService idService,
                              StorageProperties storageProperties) {
        this.resourceDomainService = resourceDomainService;
        this.resourcePolicyService = resourcePolicyService;
        this.resourceObjectMapper = resourceObjectMapper;
        this.resourceBindingMapper = resourceBindingMapper;
        this.uploadSessionStore = uploadSessionStore;
        this.storageClientDelegate = storageClientDelegate;
        this.profilesLoader = profilesLoader;
        this.idService = idService;
        this.storageProperties = storageProperties;
    }

    @Override
    public UploadPolicyView requestUploadPolicy(ResourceUploadRequest request) {
        log.info("Requesting upload policy for profile={} owner={}/{}", request.profileCode(), request.ownerType(), request.ownerId());
        if (request == null) {
            throw new ResourceUploadException("上传请求不能为空");
        }
        validateRequest(request);

        ResourceProfileSpec spec = profilesLoader.getProfile(request.profileCode());
        long tenantId = resolveTenantId();
        long requestedSize = request.sizeBytes();
        if (requestedSize <= 0 || requestedSize > spec.getMaxSizeBytes()) {
            throw new ResourceUploadException("文件大小不合法，限制为 " + spec.getMaxSizeBytes() + " 字节以内");
        }

        String fileExt = normalizeExtension(request.fileName());
        if (!spec.getAllowedExtensions().contains(fileExt)) {
            throw new ResourceUploadException("不支持的文件扩展名 " + fileExt);
        }

        String contentType = StringUtils.hasText(request.contentType())
                ? request.contentType().toLowerCase(Locale.ROOT)
                : "";
        if (!spec.getAllowedContentTypes().isEmpty() && !spec.getAllowedContentTypes().contains(contentType)) {
            throw new ResourceUploadException("不支持的文件类型 " + contentType);
        }

        String storageKey = buildStorageKey(spec, tenantId, request.ownerType(), request.ownerId(), fileExt);

        GenerateUploadPolicyRequest policyRequest = new GenerateUploadPolicyRequest();
        policyRequest.setBucketName(spec.getBucketName());
        policyRequest.setStorageKey(storageKey);
        policyRequest.setContentType(contentType);
        policyRequest.setMaxSizeBytes(spec.getMaxSizeBytes());
        policyRequest.setExpireSeconds(spec.getExpireSeconds());
        policyRequest.setAccessLevel(spec.getAccessLevel());

        StorageUploadPolicy policy;
        try {
            policy = storageClientDelegate.generateUploadPolicy(policyRequest);
        } catch (Exception ex) {
            log.error("生成上传策略失败", ex);
            throw new ResourceUploadException("生成上传策略失败", ex);
        }

        if (policy == null || policy.getUploadUrl() == null) {
            throw new ResourceUploadException("存储服务未返回上传地址");
        }

        Instant expiresAt = policy.getExpiresAt();
        if (expiresAt == null) {
            expiresAt = Instant.now().plusSeconds(spec.getExpireSeconds());
        }

        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofSeconds(Math.max(spec.getExpireSeconds(), 60L));
            expiresAt = Instant.now().plus(ttl);
        }

        String uploadToken = idService.nextId();
        UploadSessionStore.UploadSession session = new UploadSessionStore.UploadSession(
                uploadToken,
                tenantId,
                request.profileCode(),
                request.ownerType(),
                request.ownerId(),
                requestedSize,
                request.hashSha256(),
                storageKey,
                contentType,
                spec.getPurpose(),
                expiresAt);

        uploadSessionStore.save(session, ttl);
        log.info("上传策略准备完毕，token={} storageKey={}", uploadToken, storageKey);

        return new UploadPolicyView(uploadToken,
                policy.getUploadUrl(),
                copyFormFields(policy),
                expiresAt);
    }

    @Override
    public ResourceHandle completeUpload(String uploadToken, String storageKey, long sizeBytes, String hashSha256) {
        log.info("Completing upload token={}, storageKey={}", uploadToken, storageKey);
        UploadSessionStore.UploadSession session = uploadSessionStore.load(uploadToken)
                .orElseThrow(() -> new ResourceUploadException("上传会话不存在或已失效"));

        if (session.expiresAt() != null && session.expiresAt().isBefore(Instant.now())) {
            throw new ResourceUploadException("上传会话已过期");
        }

        if (!StringUtils.hasText(storageKey) || !storageKey.equals(session.storageKey())) {
            throw new ResourceUploadException("storageKey 校验失败");
        }

        if (session.expectedSize() != null && sizeBytes > session.expectedSize()) {
            throw new ResourceUploadException("实际文件大小超过预期");
        }

        String expectedHash = session.expectedHash();
        String actualHash = StringUtils.hasText(hashSha256) ? hashSha256 : expectedHash;

        ResourceProfileSpec spec = profilesLoader.getProfile(session.profileCode());
        ResourceObjectDO resourceObject = null;
        if (StringUtils.hasText(actualHash)) {
            resourceObject = resourceObjectMapper.findByTenantProfileHash(session.tenantId(), session.profileCode().name(), actualHash);
        }

        if (resourceObject == null) {
            resourceObject = new ResourceObjectDO();
            resourceObject.setTenantId(session.tenantId());
            resourceObject.setProfileCode(session.profileCode().name());
            resourceObject.setStorageProvider(storageProperties.getProvider().name());
            resourceObject.setStorageKey(session.storageKey());
            resourceObject.setSizeBytes(sizeBytes);
            resourceObject.setContentType(StringUtils.hasText(session.contentType()) ? session.contentType() : "");
            resourceObject.setFileExt(extractExtension(session.storageKey()));
            resourceObject.setHashSha256(actualHash);
            resourceObject.setAccessLevel(mapAccessLevel(spec.getAccessLevel()));
            resourceObject.setStatus(1);
            resourceObjectMapper.insert(resourceObject);
        }

        ResourceBindingDO binding = new ResourceBindingDO();
        binding.setOwnerType(session.ownerType().name());
        binding.setOwnerId(session.ownerId());
        binding.setPurpose(session.purpose().name());
        binding.setResourceObjectId(resourceObject.getId());
        binding.setSortOrder(0);
        binding.setIsMain(session.purpose() == ResourcePurpose.MAIN_LOGO);
        binding.setCreatedAt(LocalDateTime.now());
        resourceBindingMapper.insert(binding);

        uploadSessionStore.delete(uploadToken);

        GenerateDownloadUrlRequest downloadRequest = new GenerateDownloadUrlRequest();
        downloadRequest.setBucketName(spec.getBucketName());
        downloadRequest.setStorageKey(resourceObject.getStorageKey());
        downloadRequest.setAccessLevel(spec.getAccessLevel());
        downloadRequest.setExpireSeconds(spec.getExpireSeconds());

        String downloadUrl = storageClientDelegate.generateDownloadUrl(downloadRequest);
        log.info("上传完成，resourceId={}", resourceObject.getId());

        return new ResourceHandle(String.valueOf(resourceObject.getId()),
                session.profileCode(),
                downloadUrl,
                sizeBytes,
                resourceObject.getContentType());
    }

    @Override
    public ResourceHandle getMainResource(ResourceOwnerType ownerType, Long ownerId, ResourcePurpose purpose) {
        throw new UnsupportedOperationException("TODO implement resource lookup");
    }

    @Override
    public List<ResourceHandle> listResources(ResourceQuery query) {
        throw new UnsupportedOperationException("TODO implement resource listing");
    }

    @Override
    public void bindExistingObject(BindResourceCommand command) {
        throw new UnsupportedOperationException("TODO implement resource binding");
    }

    @Override
    public void unbindResource(UnbindResourceCommand command) {
        throw new UnsupportedOperationException("TODO implement resource unbind");
    }

    private void validateRequest(ResourceUploadRequest request) {
        if (request.ownerType() == null || request.ownerId() == null || request.profileCode() == null) {
            throw new ResourceUploadException("上传请求缺少必要的 owner/profile 信息");
        }
        if (!StringUtils.hasText(request.fileName())) {
            throw new ResourceUploadException("上传文件名不能为空");
        }
    }

    private long resolveTenantId() {
        String tenantRaw = TenantContext.getTenantId();
        if (!StringUtils.hasText(tenantRaw)) {
            throw new ResourceUploadException("当前租户未设置");
        }
        try {
            return Long.parseLong(tenantRaw);
        } catch (NumberFormatException ex) {
            throw new ResourceUploadException("租户 ID 无效：" + tenantRaw);
        }
    }

    private String buildStorageKey(ResourceProfileSpec spec,
                                   long tenantId,
                                   ResourceOwnerType ownerType,
                                   Long ownerId,
                                   String extension) {
        LocalDate now = LocalDate.now(ZoneId.systemDefault());
        String ulid = idService.nextId();
        return String.join("/",
                profilesLoader.getEnvironment(),
                String.valueOf(tenantId),
                spec.getBasePath(),
                String.valueOf(now.getYear()),
                String.format("%02d", now.getMonthValue()),
                String.format("%02d", now.getDayOfMonth()),
                ownerType.name().toLowerCase(Locale.ROOT) + "-" + ownerId,
                ulid + "." + extension);
    }

    private static String normalizeExtension(String fileName) {
        String ext = StringUtils.getFilenameExtension(fileName);
        if (!StringUtils.hasText(ext)) {
            throw new ResourceUploadException("文件扩展名不能为空");
        }
        return ext.toLowerCase(Locale.ROOT);
    }

    private static String extractExtension(String storageKey) {
        String ext = StringUtils.getFilenameExtension(storageKey);
        return StringUtils.hasText(ext) ? ext.toLowerCase(Locale.ROOT) : "";
    }

    private static int mapAccessLevel(AccessLevel accessLevel) {
        return switch (accessLevel) {
            case PRIVATE -> 1;
            case PUBLIC_READ -> 2;
            case INTERNAL -> 3;
        };
    }

    private static Map<String, String> copyFormFields(StorageUploadPolicy policy) {
        Map<String, String> raw = policy.getFormFields();
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyMap();
        }
        return Map.copyOf(raw);
    }
}
