package com.bluecone.app.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.id.core.Ulid128;
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
    private final IdService idService;

    @Override
    @Transactional
    public Long createTenant(CreateTenantCommand command) {
        // 1. 构建租户基础信息实体
        Tenant tenant = new Tenant();
        // 生成内部 ULID 和对外 Public ID
//        Ulid128 ulid = idService.nextUlid();
//        String publicId = idService.nextPublicId(ResourceType.TENANT);
//        tenant.setInternalId(ulid);
//        tenant.setPublicId(publicId);
        
        // 生成全局唯一的租户编码（时间戳 + 随机数）
        tenant.setTenantCode(generateTenantCode());
        // 设置租户名称
        tenant.setTenantName(command.tenantName());
        // 默认状态置为启用（1）
        tenant.setStatus(1);
        // 设置联系人姓名
        tenant.setContactPerson(command.contactPerson());
        // 设置联系人电话
        tenant.setContactPhone(command.contactPhone());
        // 设置联系人邮箱
        tenant.setContactEmail(command.contactEmail());
        // 设置备注信息
        tenant.setRemark(command.remark());
        // 插入 tenant 表，生成主键 ID
        tenantService.save(tenant);

        // 2. 构建租户主体详情（profile）
        TenantProfile profile = new TenantProfile();
        // 关联刚刚创建的租户 ID
        profile.setTenantId(tenant.getId());
        // 设置主体类型
        profile.setTenantType(command.tenantType());
        // 设置工商主体名称
        profile.setBusinessName(command.businessName());
        // 设置营业执照注册号
        profile.setBusinessLicenseNo(command.businessLicenseNo());
        // 设置营业执照图片地址
        profile.setBusinessLicenseUrl(command.businessLicenseUrl());
        // 设置法人姓名
        profile.setLegalPersonName(command.legalPersonName());
        // 设置法人身份证号
        profile.setLegalPersonIdNo(command.legalPersonIdNo());
        // 设置主体注册地址
        profile.setAddress(command.address());
        // 插入 tenant_profile 表
        tenantProfileService.save(profile);

        // 3. 初始化租户配置（如套餐 ID / 到期时间等）
        initializeDefaultSettings(tenant.getId(), command.initialPlanId(), command.planExpireAt());

        // 4. 如有传入初始化套餐，则走一遍套餐变更流程写入 billing & settings
        if (command.initialPlanId() != null) {
            changeTenantPlan(new ChangeTenantPlanCommand(
                    // 新建租户 ID
                    tenant.getId(),
                    // 初始化套餐 ID
                    command.initialPlanId(),
                    // 创建时暂不记录具体支付金额
                    null,
                    // 创建时暂不记录具体支付方式
                    null,
                    // 初始化套餐到期时间
                    command.planExpireAt(),
                    // 操作人 ID 用于审计
                    command.operatorId()));
        }

        // 5. 记录审计日志，标记“租户已创建”
        recordAudit(tenant.getId(), command.operatorId(), "TENANT_CREATED",
                "Tenant created with code " + tenant.getTenantCode());
        // 返回新建租户的主键 ID 给上层
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
                .publicId(tenant.getPublicId())
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
                    .publicId(t.getPublicId())
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
        // 构建要批量写入的配置列表
        List<TenantSettings> settings = new ArrayList<>();
        // 记录租户当前套餐 ID 的配置项
        TenantSettings planSetting = new TenantSettings();
        // 绑定租户 ID
        planSetting.setTenantId(tenantId);
        // key 为 plan.id，表示当前套餐
        planSetting.setKeyName(SETTING_PLAN_ID);
        // 如果传了套餐 ID 则使用，否则标记为 "free"（免费版）
        planSetting.setKeyValue(planId != null ? planId.toString() : "free");
        // 加入待保存列表
        settings.add(planSetting);
        // 如传入到期时间，则一并写入 plan.expireAt 配置
        if (expireAt != null) {
            TenantSettings expireSetting = new TenantSettings();
            // 绑定租户 ID
            expireSetting.setTenantId(tenantId);
            // key 为 plan.expireAt，表示套餐到期时间
            expireSetting.setKeyName(SETTING_PLAN_EXPIRE_AT);
            // 以字符串形式保存到期时间
            expireSetting.setKeyValue(expireAt.toString());
            // 加入待保存列表
            settings.add(expireSetting);
        }
        // 批量保存配置项，避免多次数据库往返
        if (!settings.isEmpty()) {
            tenantSettingsService.saveBatch(settings);
        }
    }

    private void upsertSetting(Long tenantId, String key, String value) {
        // 按租户 + key 查询已有配置
        TenantSettings settings = tenantSettingsService.lambdaQuery()
                .eq(TenantSettings::getTenantId, tenantId)
                .eq(TenantSettings::getKeyName, key)
                .one();
        // 如果不存在则新建一条
        if (settings == null) {
            settings = new TenantSettings();
            // 绑定租户 ID
            settings.setTenantId(tenantId);
            // 设置配置项 key
            settings.setKeyName(key);
        }
        // 更新配置值
        settings.setKeyValue(value);
        // 保存或更新到数据库，实现 upsert 效果
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
        // 创建一条审计日志记录
        TenantAuditLog auditLog = new TenantAuditLog();
        // 关联租户 ID
        auditLog.setTenantId(tenantId);
        // 记录操作者 ID（可为空，视安全上下文而定）
        auditLog.setOperatorId(operatorId);
        // 写入本次操作的类型编码
        auditLog.setAction(action);
        // 组装 JSON 格式的详情，兼容 MySQL JSON 字段类型
        auditLog.setDetail(buildAuditDetailJson(action, detail));
        // 记录创建时间
        auditLog.setCreatedAt(LocalDateTime.now());
        // 插入 tenant_audit_log 表
        tenantAuditLogService.save(auditLog);
    }

    /**
     * 构建审计日志 detail 字段的 JSON 字符串
     * 说明：tenant_audit_log.detail 列为 JSON 类型，必须保存合法 JSON 文本
     */
    private String buildAuditDetailJson(String action, String detail) {
        String safeAction = action == null ? "" : action.replace("\\", "\\\\").replace("\"", "\\\"");
        String safeDetail = detail == null ? "" : detail.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"action\":\"" + safeAction + "\",\"detail\":\"" + safeDetail + "\"}";
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
        // 使用当前时间（精确到秒）作为前缀，便于排序和排查问题
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        // 生成 4 位随机数，降低碰撞概率
        int random = 1000 + new Random().nextInt(9000);
        // 拼接成形如 TEN202512031230309999 的编码
        return "TEN" + LocalDateTime.now().format(formatter) + random;
    }
}
