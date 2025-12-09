package com.bluecone.app.resource.application;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.bluecone.app.core.id.IdService;
import com.bluecone.app.infra.storage.AccessLevel;
import com.bluecone.app.infra.storage.GenerateDownloadUrlRequest;
import com.bluecone.app.infra.storage.GenerateUploadPolicyRequest;
import com.bluecone.app.infra.storage.config.StorageProperties;
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
import com.bluecone.app.resource.api.exception.ResourceAccessDeniedException;
import com.bluecone.app.resource.api.exception.ResourceNotFoundException;
import com.bluecone.app.resource.api.exception.ResourceUploadException;
import com.bluecone.app.resource.domain.service.ResourceDomainService;
import com.bluecone.app.resource.domain.service.ResourcePolicyService;
import com.bluecone.app.resource.domain.service.TenantResourceQuotaService;
import com.bluecone.app.resource.infrastructure.repository.ResourceBindingDO;
import com.bluecone.app.resource.infrastructure.repository.ResourceBindingMapper;
import com.bluecone.app.resource.infrastructure.repository.ResourceObjectDO;
import com.bluecone.app.resource.infrastructure.repository.ResourceObjectMapper;
import com.bluecone.app.resource.infrastructure.session.UploadSessionStore;
import com.bluecone.app.resource.infrastructure.storage.StorageClientDelegate;
import com.bluecone.app.resource.support.ResourceProfileSpec;
import com.bluecone.app.resource.support.ResourceProfilesLoader;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资源中心 API 的默认实现，承担接口、领域、基础设施的桥接。
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
    private final TenantResourceQuotaService quotaService;
    private final MeterRegistry meterRegistry;

    public ResourceClientImpl(ResourceDomainService resourceDomainService,
                              ResourcePolicyService resourcePolicyService,
                              ResourceObjectMapper resourceObjectMapper,
                              ResourceBindingMapper resourceBindingMapper,
                              UploadSessionStore uploadSessionStore,
                              StorageClientDelegate storageClientDelegate,
                              ResourceProfilesLoader profilesLoader,
                              IdService idService,
                              StorageProperties storageProperties,
                              MeterRegistry meterRegistry,
                              TenantResourceQuotaService quotaService) {
        this.resourceDomainService = resourceDomainService;
        this.resourcePolicyService = resourcePolicyService;
        this.resourceObjectMapper = resourceObjectMapper;
        this.resourceBindingMapper = resourceBindingMapper;
        this.uploadSessionStore = uploadSessionStore;
        this.storageClientDelegate = storageClientDelegate;
        this.profilesLoader = profilesLoader;
        this.idService = idService;
        this.storageProperties = storageProperties;
        this.meterRegistry = meterRegistry;
        this.quotaService = quotaService;
    }

    @Override
    public UploadPolicyView requestUploadPolicy(ResourceUploadRequest request) {
        if (request == null) {
            throw new ResourceUploadException(ResourceUploadException.RES_UPLOAD_INVALID_ARGUMENT, "上传请求不能为空");
        }
        validateRequest(request);
        ResourceProfileCode profile = request.profileCode();
        long tenantId = resolveTenantId();
        ResourceProfileSpec spec = profilesLoader.getProfile(profile);
        boolean success = false;
        String storageKey = null;
        String uploadToken;
        try {
            quotaService.assertCanUpload(tenantId, request.sizeBytes(), profile);
            long requestedSize = request.sizeBytes();
            if (requestedSize <= 0 || requestedSize > spec.getMaxSizeBytes()) {
                throw new ResourceUploadException(ResourceUploadException.RES_UPLOAD_INVALID_ARGUMENT,
                        "文件大小不合法，限制为 " + spec.getMaxSizeBytes() + " 字节以内");
            }

            String fileExt = normalizeExtension(request.fileName());
            if (!spec.getAllowedExtensions().contains(fileExt)) {
                throw new ResourceUploadException(ResourceUploadException.RES_UPLOAD_INVALID_ARGUMENT,
                        "不支持的文件扩展名 " + fileExt);
            }

            String contentType = StringUtils.hasText(request.contentType())
                    ? request.contentType().toLowerCase(Locale.ROOT)
                    : "";
            if (!spec.getAllowedContentTypes().isEmpty() && !spec.getAllowedContentTypes().contains(contentType)) {
                throw new ResourceUploadException(ResourceUploadException.RES_UPLOAD_INVALID_ARGUMENT,
                        "不支持的文件类型 " + contentType);
            }

            storageKey = buildStorageKey(spec, tenantId, request.ownerType(), request.ownerId(), fileExt);
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
                log.error("[ResourceUploadPolicy] tenant={} owner={}/{} profile={} storageKey={} errorCode={}",
                        tenantId, request.ownerType(), request.ownerId(), profile, storageKey, ResourceUploadException.RES_UPLOAD_STORAGE_ERROR, ex);
                throw new ResourceUploadException(ResourceUploadException.RES_UPLOAD_STORAGE_ERROR, "生成上传策略失败", ex);
            }

            if (policy == null || policy.getUploadUrl() == null) {
                throw new ResourceUploadException(ResourceUploadException.RES_UPLOAD_STORAGE_ERROR, "存储服务未返回上传地址");
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

            uploadToken = idService.nextId();
            UploadSessionStore.UploadSession session = new UploadSessionStore.UploadSession(
                    uploadToken,
                    tenantId,
                    profile,
                    request.ownerType(),
                    request.ownerId(),
                    request.sizeBytes(),
                    request.hashSha256(),
                    storageKey,
                    contentType,
                    spec.getPurpose(),
                    expiresAt);

            uploadSessionStore.save(session, ttl);
            log.info("[ResourceUploadPolicy] tenant={} ownerType={} ownerId={} profile={} fileName={} sizeBytes={} token={} storageKey={}",
                    tenantId, request.ownerType(), request.ownerId(), profile, request.fileName(), request.sizeBytes(), uploadToken, storageKey);
            success = true;
            return new UploadPolicyView(uploadToken, policy.getUploadUrl(), copyFormFields(policy), expiresAt);
        } catch (ResourceUploadException ex) {
            recordPolicyMetric(profile, tenantId, "fail");
            throw ex;
        } finally {
            if (success) {
                recordPolicyMetric(profile, tenantId, "success");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceHandle completeUpload(String uploadToken, String storageKey, long sizeBytes, String hashSha256) {
        boolean success = false;
        ResourceProfileCode profile = null;
        long tenantId = currentTenantId();
        String provider = storageProperties.getProvider().name();
        try {
            UploadSessionStore.UploadSession session = uploadSessionStore.load(uploadToken)
                    .orElseThrow(() -> new ResourceUploadException(ResourceUploadException.RES_UPLOAD_SESSION_EXPIRED, "上传会话不存在或已失效"));

            if (!tenantIdEquals(tenantId, session.tenantId())) {
                log.warn("[ResourceUpload] tenant mismatch token={} sessionTenant={} currentTenant={}", uploadToken, session.tenantId(), tenantId);
                throw new ResourceAccessDeniedException("无权完成该上传");
            }

            if (session.expiresAt() != null && session.expiresAt().isBefore(Instant.now())) {
                throw new ResourceUploadException(ResourceUploadException.RES_UPLOAD_SESSION_EXPIRED, "上传会话已过期");
            }

            if (!StringUtils.hasText(storageKey) || !storageKey.equals(session.storageKey())) {
                throw new ResourceUploadException(ResourceUploadException.RES_UPLOAD_INVALID_ARGUMENT, "storageKey 校验失败");
            }

            if (session.expectedSize() != null && sizeBytes > session.expectedSize()) {
                throw new ResourceUploadException(ResourceUploadException.RES_UPLOAD_INVALID_ARGUMENT, "实际文件大小超过预期");
            }

            String expectedHash = session.expectedHash();
            String actualHash = StringUtils.hasText(hashSha256) ? hashSha256 : expectedHash;
            profile = session.profileCode();
            ResourceProfileSpec spec = profilesLoader.getProfile(profile);
            ResourceObjectDO resourceObject = null;
            if (StringUtils.hasText(actualHash)) {
                resourceObject = resourceObjectMapper.findByTenantProfileHash(session.tenantId(), profile.name(), actualHash);
            }

            if (resourceObject == null) {
                resourceObject = new ResourceObjectDO();
                resourceObject.setTenantId(session.tenantId());
                resourceObject.setProfileCode(profile.name());
                resourceObject.setStorageProvider(provider);
                resourceObject.setStorageKey(session.storageKey());
                resourceObject.setSizeBytes(sizeBytes);
                resourceObject.setContentType(StringUtils.hasText(session.contentType()) ? session.contentType() : "");
                resourceObject.setFileExt(extractExtension(session.storageKey()));
                resourceObject.setHashSha256(actualHash);
                resourceObject.setAccessLevel(mapAccessLevel(spec.getAccessLevel()));
                resourceObject.setStatus(1);
                resourceObjectMapper.insert(resourceObject);
                log.info("[ResourceObject] tenant={} profile={} storageKey={} created", tenantId, profile, session.storageKey());
            } else {
                log.info("[ResourceObject] tenant={} profile={} resourceId={} reused", tenantId, profile, resourceObject.getId());
            }

            Long resourceObjectId = resourceObject.getId();
            ResourceBindingDO existingBinding = resourceBindingMapper.findByOwnerAndObject(
                    tenantId,
                    session.ownerType().name(),
                    session.ownerId(),
                    session.purpose().name(),
                    resourceObjectId);
            if (existingBinding != null) {
                log.info("[ResourceBinding] tenant={} owner={}/{} purpose={} resource={} reused bindingId={}",
                        tenantId, session.ownerType(), session.ownerId(), session.purpose(), resourceObjectId, existingBinding.getId());
            } else {
                ResourceBindingDO binding = new ResourceBindingDO();
                binding.setTenantId(tenantId);
                binding.setOwnerType(session.ownerType().name());
                binding.setOwnerId(session.ownerId());
                binding.setPurpose(session.purpose().name());
                binding.setResourceObjectId(resourceObjectId);
                binding.setSortOrder(0);
                binding.setIsMain(session.purpose() == ResourcePurpose.MAIN_LOGO);
                binding.setCreatedAt(LocalDateTime.now());
                resourceBindingMapper.insert(binding);
                log.info("[ResourceBinding] tenant={} owner={}/{} purpose={} resource={} bindingId={} created",
                        tenantId, session.ownerType(), session.ownerId(), session.purpose(), resourceObjectId, binding.getId());
            }

            quotaService.consumeQuota(tenantId, sizeBytes, profile);
            uploadSessionStore.delete(uploadToken);
            ResourceHandle handle = buildHandleFromObject(resourceObject);
            log.info("[ResourceUploadComplete] tenant={} owner={}/{} profile={} resourceId={} storageProvider={} storageKey={} sizeBytes={}",
                    tenantId, session.ownerType(), session.ownerId(), profile, resourceObjectId, provider, session.storageKey(), sizeBytes);
            success = true;
            return handle;
        } catch (ResourceUploadException ex) {
            if (profile != null) {
                recordUploadMetric(profile, tenantId, provider, "fail");
            }
            throw ex;
        } finally {
            if (success && profile != null) {
                recordUploadMetric(profile, tenantId, provider, "success");
                recordUploadBytes(profile, sizeBytes);
            }
        }
    }

    @Override
    public ResourceHandle getMainResource(ResourceOwnerType ownerType, Long ownerId, ResourcePurpose purpose) {
        ensureOwnerInfo(ownerType, ownerId, purpose);
        long tenantId = currentTenantId();
        List<ResourceBindingDO> bindings = selectBindings(tenantId, ownerType, ownerId, purpose, 1);
        if (bindings.isEmpty()) {
            log.debug("[ResourceGetMain] tenant={} owner={}/{} purpose={} missing binding", tenantId, ownerType, ownerId, purpose);
            return null;
        }
        ResourceBindingDO binding = bindings.get(0);
        ResourceObjectDO object = resourceObjectMapper.selectById(binding.getResourceObjectId());
        ensureTenant(object);
        return buildHandleFromObject(object);
    }

    @Override
    public List<ResourceHandle> listResources(ResourceQuery query) {
        ensureOwnerInfo(query.ownerType(), query.ownerId(), query.purpose());
        long tenantId = currentTenantId();
        List<ResourceBindingDO> bindings = selectBindings(tenantId, query.ownerType(), query.ownerId(), query.purpose(), null);
        if (bindings.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = bindings.stream().map(ResourceBindingDO::getResourceObjectId).toList();
        List<ResourceObjectDO> objects = resourceObjectMapper.selectBatchIds(ids);
        Map<Long, ResourceObjectDO> objectMap = objects.stream().collect(Collectors.toMap(ResourceObjectDO::getId, obj -> obj));
        List<ResourceHandle> handles = new ArrayList<>();
        for (ResourceBindingDO binding : bindings) {
            ResourceObjectDO object = objectMap.get(binding.getResourceObjectId());
            if (object == null) {
                log.warn("[ResourceList] bindingId={} missing object", binding.getId());
                continue;
            }
            try {
                ensureTenant(object);
                handles.add(buildHandleFromObject(object));
            } catch (ResourceAccessDeniedException ex) {
                log.warn("[ResourceList] tenant={} bindingId={} access denied", tenantId, binding.getId());
            }
        }
        return handles;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindExistingObject(BindResourceCommand command) {
        ensureOwnerInfo(command.ownerType(), command.ownerId(), command.purpose());
        long tenantId = currentTenantId();
        log.info("[ResourceBinding] tenant={} owner={}/{} purpose={} resource={}",
                tenantId, command.ownerType(), command.ownerId(), command.purpose(), command.resourceObjectId());
        Long resourceObjectId = parseResourceObjectId(command.resourceObjectId());
        ResourceObjectDO object = resourceObjectMapper.selectById(resourceObjectId);
        ensureTenant(object);

        ResourceBindingDO existing = resourceBindingMapper.findByOwnerAndObject(tenantId,
                command.ownerType().name(),
                command.ownerId(),
                command.purpose().name(),
                resourceObjectId);
        if (existing != null) {
            log.info("[ResourceBinding] tenant={} existing bindingId={} refreshed", tenantId, existing.getId());
            UpdateWrapper<ResourceBindingDO> update = new UpdateWrapper<>();
            update.eq("id", existing.getId());
            ResourceBindingDO updatePayload = new ResourceBindingDO();
            if (command.sortOrder() != null) {
                updatePayload.setSortOrder(command.sortOrder());
            }
            if (command.isMain() != null) {
                updatePayload.setIsMain(command.isMain());
            }
            resourceBindingMapper.update(updatePayload, update);
            if (Boolean.TRUE.equals(command.isMain())) {
                resetOtherMainBindings(tenantId, command.ownerType(), command.ownerId(), command.purpose(), resourceObjectId);
            }
            return;
        }

        if (Boolean.TRUE.equals(command.isMain())) {
            resetOtherMainBindings(tenantId, command.ownerType(), command.ownerId(), command.purpose(), null);
        }

        ResourceBindingDO binding = new ResourceBindingDO();
        binding.setTenantId(tenantId);
        binding.setOwnerType(command.ownerType().name());
        binding.setOwnerId(command.ownerId());
        binding.setPurpose(command.purpose().name());
        binding.setResourceObjectId(resourceObjectId);
        binding.setSortOrder(command.sortOrder() != null ? command.sortOrder() : 0);
        binding.setIsMain(Boolean.TRUE.equals(command.isMain()));
        binding.setCreatedAt(LocalDateTime.now());
        resourceBindingMapper.insert(binding);
        log.info("[ResourceBinding] tenant={} owner={}/{} purpose={} resource={} bindingId={} created",
                tenantId, command.ownerType(), command.ownerId(), command.purpose(), resourceObjectId, binding.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindResource(UnbindResourceCommand command) {
        ensureOwnerInfo(command.ownerType(), command.ownerId(), command.purpose());
        long tenantId = currentTenantId();
        log.info("[ResourceUnbind] tenant={} owner={}/{} purpose={} resource={}", tenantId, command.ownerType(), command.ownerId(), command.purpose(), command.resourceObjectId());
        QueryWrapper<ResourceBindingDO> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId)
                .eq("owner_type", command.ownerType().name())
                .eq("owner_id", command.ownerId())
                .eq("purpose", command.purpose().name());
        if (StringUtils.hasText(command.resourceObjectId())) {
            wrapper.eq("resource_object_id", parseResourceObjectId(command.resourceObjectId()));
        }
        if (command.sortOrder() != null) {
            wrapper.eq("sort_order", command.sortOrder());
        }
        if (command.isMain() != null) {
            wrapper.eq("is_main", command.isMain());
        }
        resourceBindingMapper.delete(wrapper);
    }

    private void resetOtherMainBindings(long tenantId,
                                        ResourceOwnerType ownerType,
                                        Long ownerId,
                                        ResourcePurpose purpose,
                                        Long skipResourceObjectId) {
        UpdateWrapper<ResourceBindingDO> reset = new UpdateWrapper<>();
        reset.eq("tenant_id", tenantId)
                .eq("owner_type", ownerType.name())
                .eq("owner_id", ownerId)
                .eq("purpose", purpose.name());
        if (skipResourceObjectId != null) {
            reset.ne("resource_object_id", skipResourceObjectId);
        }
        ResourceBindingDO payload = new ResourceBindingDO();
        payload.setIsMain(false);
        resourceBindingMapper.update(payload, reset);
    }

    private void validateRequest(ResourceUploadRequest request) {
        if (request.ownerType() == null || request.ownerId() == null || request.profileCode() == null) {
            throw new ResourceUploadException(ResourceUploadException.RES_UPLOAD_INVALID_ARGUMENT, "上传请求缺少必要的 owner/profile 信息");
        }
        if (!StringUtils.hasText(request.fileName())) {
            throw new ResourceUploadException(ResourceUploadException.RES_UPLOAD_INVALID_ARGUMENT, "上传文件名不能为空");
        }
    }

    private void ensureOwnerInfo(ResourceOwnerType ownerType, Long ownerId, ResourcePurpose purpose) {
        if (ownerType == null || ownerId == null || purpose == null) {
            throw new ResourceUploadException(ResourceUploadException.RES_UPLOAD_INVALID_ARGUMENT, "缺少 owner 或 purpose 信息");
        }
    }

    private long resolveTenantId() {
        try {
            return currentTenantId();
        } catch (ResourceAccessDeniedException ex) {
            throw new ResourceUploadException(ex.getMessage(), ex);
        }
    }

    private long currentTenantId() {
        String tenantRaw = TenantContext.getTenantId();
        if (!StringUtils.hasText(tenantRaw)) {
            throw new ResourceAccessDeniedException("当前租户未设置");
        }
        try {
            return Long.parseLong(tenantRaw);
        } catch (NumberFormatException ex) {
            throw new ResourceAccessDeniedException("租户 ID 无效：" + tenantRaw);
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
            throw new ResourceUploadException(ResourceUploadException.RES_UPLOAD_INVALID_ARGUMENT, "文件扩展名不能为空");
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

    private List<ResourceBindingDO> selectBindings(long tenantId,
                                                   ResourceOwnerType ownerType,
                                                   Long ownerId,
                                                   ResourcePurpose purpose,
                                                   Integer limit) {
        if (ownerType == null || ownerId == null || purpose == null) {
            return Collections.emptyList();
        }
        QueryWrapper<ResourceBindingDO> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId)
                .eq("owner_type", ownerType.name())
                .eq("owner_id", ownerId)
                .eq("purpose", purpose.name())
                .orderByDesc("is_main")
                .orderByAsc("sort_order");
        if (limit != null && limit > 0) {
            wrapper.last(" LIMIT " + limit);
        }
        return resourceBindingMapper.selectList(wrapper);
    }

    private ResourceHandle buildHandleFromObject(ResourceObjectDO object) {
        ResourceProfileCode profileCode = ResourceProfileCode.valueOf(object.getProfileCode());
        ResourceProfileSpec spec = profilesLoader.getProfile(profileCode);
        GenerateDownloadUrlRequest downloadRequest = new GenerateDownloadUrlRequest();
        downloadRequest.setBucketName(spec.getBucketName());
        downloadRequest.setStorageKey(object.getStorageKey());
        downloadRequest.setAccessLevel(spec.getAccessLevel());
        downloadRequest.setExpireSeconds(spec.getExpireSeconds());
        String downloadUrl = storageClientDelegate.generateDownloadUrl(downloadRequest);
        recordDownloadMetric(profileCode, spec.getAccessLevel());
        return new ResourceHandle(String.valueOf(object.getId()),
                profileCode,
                downloadUrl,
                object.getSizeBytes(),
                object.getContentType());
    }

    private void ensureTenant(ResourceObjectDO object) {
        if (object == null) {
            throw new ResourceNotFoundException("资源对象不存在");
        }
        Long objectTenantId = object.getTenantId();
        if (objectTenantId == null) {
            throw new ResourceAccessDeniedException("资源所属租户未设置");
        }
        long tenantId = currentTenantId();
        if (!objectTenantId.equals(tenantId)) {
            log.warn("[ResourceAccessDenied] tenant={} objectTenant={}", tenantId, objectTenantId);
            throw new ResourceAccessDeniedException("无权访问该资源");
        }
    }

    private void recordPolicyMetric(ResourceProfileCode profile, long tenantId, String result) {
        if (meterRegistry != null) {
            meterRegistry.counter("resource.upload.policy.requests",
                    Tags.of("profile", profile.name(), "tenant", String.valueOf(tenantId), "result", result))
                    .increment();
        }
    }

    private void recordUploadMetric(ResourceProfileCode profile, long tenantId, String provider, String result) {
        if (meterRegistry != null) {
            meterRegistry.counter("resource.upload.completed",
                    Tags.of("profile", profile.name(), "tenant", String.valueOf(tenantId), "provider", provider, "result", result))
                    .increment();
        }
    }

    private void recordUploadBytes(ResourceProfileCode profile, long bytes) {
        if (meterRegistry != null && bytes > 0) {
            meterRegistry.summary("resource.upload.bytes",
                    Tags.of("profile", profile.name()))
                    .record(bytes);
        }
    }

    private void recordDownloadMetric(ResourceProfileCode profile, AccessLevel accessLevel) {
        if (meterRegistry != null) {
            meterRegistry.counter("resource.download.url.generated",
                    Tags.of("profile", profile.name(), "accessLevel", accessLevel.name()))
                    .increment();
        }
    }

    private Long parseResourceObjectId(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new ResourceUploadException(ResourceUploadException.RES_UPLOAD_INVALID_ARGUMENT, "资源对象 ID 不能为空");
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException ex) {
            throw new ResourceUploadException(ResourceUploadException.RES_UPLOAD_INVALID_ARGUMENT, "非法资源对象 ID：" + raw);
        }
    }

    private static boolean tenantIdEquals(Long left, Long right) {
        return left != null && left.equals(right);
    }
}
