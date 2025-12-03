package com.bluecone.app.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.tenant.dao.entity.Tenant;
import com.bluecone.app.tenant.dao.entity.TenantAuditLog;
import com.bluecone.app.tenant.dao.entity.TenantBilling;
import com.bluecone.app.tenant.dao.entity.TenantMedia;
import com.bluecone.app.tenant.dao.entity.TenantPlan;
import com.bluecone.app.tenant.dao.entity.TenantPlatformAccount;
import com.bluecone.app.tenant.dao.entity.TenantProfile;
import com.bluecone.app.tenant.dao.entity.TenantSettings;
import com.bluecone.app.tenant.dao.service.ITenantAuditLogService;
import com.bluecone.app.tenant.dao.service.ITenantBillingService;
import com.bluecone.app.tenant.dao.service.ITenantMediaService;
import com.bluecone.app.tenant.dao.service.ITenantPlanService;
import com.bluecone.app.tenant.dao.service.ITenantPlatformAccountService;
import com.bluecone.app.tenant.dao.service.ITenantProfileService;
import com.bluecone.app.tenant.dao.service.ITenantService;
import com.bluecone.app.tenant.dao.service.ITenantSettingsService;
import com.bluecone.app.tenant.model.TenantDetail;
import com.bluecone.app.tenant.model.TenantMediaView;
import com.bluecone.app.tenant.model.TenantPlanInfo;
import com.bluecone.app.tenant.model.TenantPlatformAccountView;
import com.bluecone.app.tenant.model.TenantSummary;
import com.bluecone.app.tenant.model.TenantVerificationInfo;
import com.bluecone.app.tenant.model.command.ChangeTenantPlanCommand;
import com.bluecone.app.tenant.model.command.CreateTenantCommand;
import com.bluecone.app.tenant.model.command.UpdateTenantBasicInfoCommand;
import com.bluecone.app.tenant.model.command.UpdateTenantPlatformAccountCommand;
import com.bluecone.app.tenant.model.command.UpdateTenantProfileCommand;
import com.bluecone.app.tenant.model.query.TenantQuery;
import com.bluecone.app.tenant.service.TenantApplicationService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 租户聚合应用服务，实现跨表编排与审计。
 *
 * 设计约束：
 * 1) 编排而非堆业务到 Controller：一次方法调用内部完成多表写入/读取，保持上层简洁。
 * 2) 审计内建：所有变更入口都写入 tenant_audit_log，方便后续稽核。
 * 3) Upsert/幂等：平台账号按平台类型 upsert，profile 不存在自动插入。
 * 4) 可扩展：settings 采用 key-value，便于套餐等配置扩展；留有缓存/读写分离切入点。
 */
@Service
@RequiredArgsConstructor
public class TenantApplicationServiceImpl implements TenantApplicationService {

    private static final String SETTING_PLAN_ID = "plan.id";
    private static final String SETTING_PLAN_EXPIRE_AT = "plan.expireAt";

    private final ITenantService tenantService;
    private final ITenantProfileService tenantProfileService;
    private final ITenantPlatformAccountService tenantPlatformAccountService;
    private final ITenantSettingsService tenantSettingsService;
    private final ITenantPlanService tenantPlanService;
    private final ITenantBillingService tenantBillingService;
    private final ITenantMediaService tenantMediaService;
    private final ITenantAuditLogService tenantAuditLogService;

    @Override
    @Transactional
    public Long createTenant(CreateTenantCommand command) {
        Tenant tenant = new Tenant();
        tenant.setTenantCode(generateTenantCode());
        tenant.setTenantName(command.tenantName());
        tenant.setStatus(1);
        tenant.setContactPerson(command.contactPerson());
        tenant.setContactPhone(command.contactPhone());
        tenant.setContactEmail(command.contactEmail());
        tenant.setRemark(command.remark());
        tenantService.save(tenant);

        TenantProfile profile = new TenantProfile();
        profile.setTenantId(tenant.getId());
        profile.setTenantType(command.tenantType());
        profile.setBusinessName(command.businessName());
        profile.setBusinessLicenseNo(command.businessLicenseNo());
        profile.setBusinessLicenseUrl(command.businessLicenseUrl());
        profile.setLegalPersonName(command.legalPersonName());
        profile.setLegalPersonIdNo(command.legalPersonIdNo());
        profile.setAddress(command.address());
        tenantProfileService.save(profile);

        initializeDefaultSettings(tenant.getId(), command.initialPlanId(), command.planExpireAt());

        if (command.initialPlanId() != null) {
            changeTenantPlan(new ChangeTenantPlanCommand(
                    tenant.getId(),
                    command.initialPlanId(),
                    null,
                    null,
                    command.planExpireAt(),
                    command.operatorId()));
        }

        recordAudit(tenant.getId(), command.operatorId(), "TENANT_CREATED",
                "Tenant created with code " + tenant.getTenantCode());
        return tenant.getId();
    }

    @Override
    @Transactional
    public void updateTenantBasicInfo(UpdateTenantBasicInfoCommand command) {
        Tenant tenant = tenantService.getById(command.tenantId());
        if (tenant == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND.getCode(), "租户不存在");
        }
        if (StringUtils.hasText(command.tenantName())) {
            tenant.setTenantName(command.tenantName());
        }
        if (command.status() != null) {
            tenant.setStatus(command.status());
        }
        tenant.setContactPerson(command.contactPerson());
        tenant.setContactPhone(command.contactPhone());
        tenant.setContactEmail(command.contactEmail());
        tenant.setRemark(command.remark());
        tenantService.updateById(tenant);

        recordAudit(command.tenantId(), command.operatorId(), "TENANT_BASIC_UPDATED", "更新基础信息");
    }

    @Override
    @Transactional
    public void updateTenantProfile(UpdateTenantProfileCommand command) {
        Tenant tenant = tenantService.getById(command.tenantId());
        if (tenant == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND.getCode(), "租户不存在");
        }
        TenantProfile profile = tenantProfileService.lambdaQuery()
                .eq(TenantProfile::getTenantId, command.tenantId())
                .one();
        if (profile == null) {
            profile = new TenantProfile();
            profile.setTenantId(command.tenantId());
        }
        profile.setTenantType(command.tenantType());
        profile.setBusinessName(command.businessName());
        profile.setBusinessLicenseNo(command.businessLicenseNo());
        profile.setBusinessLicenseUrl(command.businessLicenseUrl());
        profile.setLegalPersonName(command.legalPersonName());
        profile.setLegalPersonIdNo(command.legalPersonIdNo());
        profile.setAddress(command.address());
        profile.setVerificationStatus(command.verificationStatus());
        profile.setVerificationReason(command.verificationReason());
        tenantProfileService.saveOrUpdate(profile);

        recordAudit(command.tenantId(), command.operatorId(), "TENANT_PROFILE_UPDATED", "更新主体/认证资料");
    }

    @Override
    @Transactional
    public void updateTenantPlatformAccount(UpdateTenantPlatformAccountCommand command) {
        Tenant tenant = tenantService.getById(command.tenantId());
        if (tenant == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND.getCode(), "租户不存在");
        }
        TenantPlatformAccount account = tenantPlatformAccountService.lambdaQuery()
                .eq(TenantPlatformAccount::getTenantId, command.tenantId())
                .eq(TenantPlatformAccount::getPlatformType, command.platformType())
                .one();
        if (account == null) {
            account = new TenantPlatformAccount();
            account.setTenantId(command.tenantId());
            account.setPlatformType(command.platformType());
        }
        account.setPlatformAccountId(command.platformAccountId());
        account.setAccountName(command.accountName());
        account.setCredential(command.credential());
        account.setStatus(command.status());
        account.setExpireAt(command.expireAt());
        tenantPlatformAccountService.saveOrUpdate(account);

        recordAudit(command.tenantId(), command.operatorId(), "TENANT_PLATFORM_UPDATED",
                "更新平台账号：" + command.platformType());
    }

    @Override
    @Transactional
    public void changeTenantPlan(ChangeTenantPlanCommand command) {
        Tenant tenant = tenantService.getById(command.tenantId());
        if (tenant == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND.getCode(), "租户不存在");
        }
        TenantPlan plan = tenantPlanService.getById(command.planId());
        if (plan == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND.getCode(), "套餐不存在");
        }
        TenantBilling billing = new TenantBilling();
        billing.setTenantId(command.tenantId());
        billing.setPlanId(command.planId());
        billing.setPayAmount(command.payAmount() != null ? command.payAmount() : BigDecimal.ZERO);
        billing.setPayMethod(StringUtils.hasText(command.payMethod()) ? command.payMethod() : "manual");
        billing.setPayTime(LocalDateTime.now());
        billing.setExpireAt(command.expireAt());
        billing.setStatus((byte) 1);
        tenantBillingService.save(billing);

        upsertSetting(command.tenantId(), SETTING_PLAN_ID, String.valueOf(command.planId()));
        if (command.expireAt() != null) {
            upsertSetting(command.tenantId(), SETTING_PLAN_EXPIRE_AT, command.expireAt().toString());
        }

        recordAudit(command.tenantId(), command.operatorId(), "TENANT_PLAN_CHANGED",
                "切换套餐至 " + plan.getPlanName());
    }

    @Override
    @Transactional(readOnly = true)
    public TenantDetail getTenantDetail(Long tenantId) {
        Tenant tenant = tenantService.getById(tenantId);
        if (tenant == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND.getCode(), "租户不存在");
        }
        TenantProfile profile = tenantProfileService.lambdaQuery()
                .eq(TenantProfile::getTenantId, tenantId)
                .one();
        Map<String, String> settings = loadSettings(tenantId);
        TenantPlanInfo planInfo = resolvePlanInfo(tenantId, settings);
        List<TenantPlatformAccountView> accountViews = tenantPlatformAccountService.lambdaQuery()
                .eq(TenantPlatformAccount::getTenantId, tenantId)
                .list()
                .stream()
                .map(a -> TenantPlatformAccountView.builder()
                        .id(a.getId())
                        .platformType(a.getPlatformType())
                        .platformAccountId(a.getPlatformAccountId())
                        .accountName(a.getAccountName())
                        .status(a.getStatus())
                        .expireAt(a.getExpireAt())
                        .build())
                .toList();
        List<TenantMediaView> mediaViews = tenantMediaService.lambdaQuery()
                .eq(TenantMedia::getTenantId, tenantId)
                .list()
                .stream()
                .map(media -> TenantMediaView.builder()
                        .id(media.getId())
                        .mediaType(media.getMediaType())
                        .url(media.getUrl())
                        .description(media.getDescription())
                        .createdAt(media.getCreatedAt())
                        .build())
                .toList();

        return TenantDetail.builder()
                .tenantId(tenant.getId())
                .tenantCode(tenant.getTenantCode())
                .tenantName(tenant.getTenantName())
                .status(tenant.getStatus())
                .contactPerson(tenant.getContactPerson())
                .contactPhone(tenant.getContactPhone())
                .contactEmail(tenant.getContactEmail())
                .remark(tenant.getRemark())
                .verificationInfo(buildVerification(profile))
                .planInfo(planInfo)
                .platformAccounts(accountViews)
                .mediaList(mediaViews)
                .settings(settings)
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantSummary> listTenantSummary(TenantQuery query) {
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.keyword())) {
            wrapper.and(w -> w.like(Tenant::getTenantName, query.keyword())
                    .or().like(Tenant::getTenantCode, query.keyword()));
        }
        if (query.status() != null) {
            wrapper.eq(Tenant::getStatus, query.status());
        }
        Page<Tenant> page = tenantService.page(new Page<>(query.pageNo(), query.pageSize()), wrapper);
        List<Tenant> tenants = page.getRecords();
        if (CollectionUtils.isEmpty(tenants)) {
            return Collections.emptyList();
        }
        Map<Long, TenantPlanInfo> planInfoByTenant = resolvePlanInfoForTenants(
                tenants.stream().map(Tenant::getId).toList());
        return tenants.stream().map(t -> {
            TenantPlanInfo planInfo = planInfoByTenant.get(t.getId());
            return TenantSummary.builder()
                    .tenantId(t.getId())
                    .tenantCode(t.getTenantCode())
                    .tenantName(t.getTenantName())
                    .status(t.getStatus())
                    .contactPerson(t.getContactPerson())
                    .contactPhone(t.getContactPhone())
                    .planId(planInfo != null ? planInfo.getPlanId() : null)
                    .planName(planInfo != null ? planInfo.getPlanName() : null)
                    .planExpireAt(planInfo != null ? planInfo.getExpireAt() : null)
                    .createdAt(t.getCreatedAt())
                    .build();
        }).toList();
    }

    private void initializeDefaultSettings(Long tenantId, Long planId, LocalDateTime expireAt) {
        List<TenantSettings> settings = new ArrayList<>();
        TenantSettings planSetting = new TenantSettings();
        planSetting.setTenantId(tenantId);
        planSetting.setKeyName(SETTING_PLAN_ID);
        planSetting.setKeyValue(planId != null ? planId.toString() : "free");
        settings.add(planSetting);
        if (expireAt != null) {
            TenantSettings expireSetting = new TenantSettings();
            expireSetting.setTenantId(tenantId);
            expireSetting.setKeyName(SETTING_PLAN_EXPIRE_AT);
            expireSetting.setKeyValue(expireAt.toString());
            settings.add(expireSetting);
        }
        if (!settings.isEmpty()) {
            tenantSettingsService.saveBatch(settings);
        }
    }

    private void upsertSetting(Long tenantId, String key, String value) {
        TenantSettings settings = tenantSettingsService.lambdaQuery()
                .eq(TenantSettings::getTenantId, tenantId)
                .eq(TenantSettings::getKeyName, key)
                .one();
        if (settings == null) {
            settings = new TenantSettings();
            settings.setTenantId(tenantId);
            settings.setKeyName(key);
        }
        settings.setKeyValue(value);
        tenantSettingsService.saveOrUpdate(settings);
    }

    private Map<String, String> loadSettings(Long tenantId) {
        return tenantSettingsService.lambdaQuery()
                .eq(TenantSettings::getTenantId, tenantId)
                .list()
                .stream()
                .collect(Collectors.toMap(TenantSettings::getKeyName, TenantSettings::getKeyValue, (a, b) -> b));
    }

    private TenantPlanInfo resolvePlanInfo(Long tenantId, Map<String, String> settings) {
        Long planId = parseLong(settings.get(SETTING_PLAN_ID));
        LocalDateTime expireAt = parseDate(settings.get(SETTING_PLAN_EXPIRE_AT));
        if (planId == null) {
            TenantBilling latest = tenantBillingService.lambdaQuery()
                    .eq(TenantBilling::getTenantId, tenantId)
                    .orderByDesc(TenantBilling::getCreatedAt)
                    .last("limit 1")
                    .one();
            if (latest != null) {
                planId = latest.getPlanId();
                expireAt = latest.getExpireAt();
            }
        }
        if (planId == null) {
            return null;
        }
        TenantPlan plan = tenantPlanService.getById(planId);
        if (plan == null) {
            return null;
        }
        return TenantPlanInfo.builder()
                .planId(plan.getId())
                .planName(plan.getPlanName())
                .price(plan.getPrice())
                .features(plan.getFeatures())
                .status(plan.getStatus())
                .expireAt(expireAt)
                .build();
    }

    private Map<Long, TenantPlanInfo> resolvePlanInfoForTenants(List<Long> tenantIds) {
        if (CollectionUtils.isEmpty(tenantIds)) {
            return Collections.emptyMap();
        }
        List<TenantSettings> settings = tenantSettingsService.lambdaQuery()
                .in(TenantSettings::getTenantId, tenantIds)
                .in(TenantSettings::getKeyName, List.of(SETTING_PLAN_ID, SETTING_PLAN_EXPIRE_AT))
                .list();
        Map<Long, Map<String, String>> settingsByTenant = new HashMap<>();
        for (TenantSettings setting : settings) {
            settingsByTenant
                    .computeIfAbsent(setting.getTenantId(), k -> new HashMap<>())
                    .put(setting.getKeyName(), setting.getKeyValue());
        }
        Map<Long, Long> planIdMap = new HashMap<>();
        Map<Long, LocalDateTime> expireAtMap = new HashMap<>();
        settingsByTenant.forEach((tenantId, map) -> {
            Long planId = parseLong(map.get(SETTING_PLAN_ID));
            LocalDateTime expireAt = parseDate(map.get(SETTING_PLAN_EXPIRE_AT));
            if (planId != null) {
                planIdMap.put(tenantId, planId);
            }
            if (expireAt != null) {
                expireAtMap.put(tenantId, expireAt);
            }
        });
        List<Long> planIds = planIdMap.values().stream().filter(Objects::nonNull).distinct().toList();
        Map<Long, TenantPlan> planMap = planIds.isEmpty()
                ? Collections.emptyMap()
                : tenantPlanService.listByIds(planIds).stream()
                        .collect(Collectors.toMap(TenantPlan::getId, p -> p));
        Map<Long, TenantPlanInfo> result = new HashMap<>();
        planIdMap.forEach((tenantId, planId) -> {
            TenantPlan plan = planMap.get(planId);
            if (plan != null) {
                result.put(tenantId, TenantPlanInfo.builder()
                        .planId(plan.getId())
                        .planName(plan.getPlanName())
                        .price(plan.getPrice())
                        .features(plan.getFeatures())
                        .status(plan.getStatus())
                        .expireAt(expireAtMap.get(tenantId))
                        .build());
            }
        });
        return result;
    }

    private void recordAudit(Long tenantId, Long operatorId, String action, String detail) {
        TenantAuditLog auditLog = new TenantAuditLog();
        auditLog.setTenantId(tenantId);
        auditLog.setOperatorId(operatorId);
        auditLog.setAction(action);
        auditLog.setDetail(detail);
        auditLog.setCreatedAt(LocalDateTime.now());
        tenantAuditLogService.save(auditLog);
    }

    private TenantVerificationInfo buildVerification(TenantProfile profile) {
        if (profile == null) {
            return null;
        }
        return TenantVerificationInfo.builder()
                .tenantType(profile.getTenantType())
                .businessName(profile.getBusinessName())
                .businessLicenseNo(profile.getBusinessLicenseNo())
                .businessLicenseUrl(profile.getBusinessLicenseUrl())
                .legalPersonName(profile.getLegalPersonName())
                .legalPersonIdNo(profile.getLegalPersonIdNo())
                .address(profile.getAddress())
                .verificationStatus(profile.getVerificationStatus())
                .verificationReason(profile.getVerificationReason())
                .build();
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private LocalDateTime parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String generateTenantCode() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        int random = 1000 + new Random().nextInt(9000);
        return "TEN" + LocalDateTime.now().format(formatter) + random;
    }
}
