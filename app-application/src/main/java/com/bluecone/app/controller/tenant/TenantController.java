package com.bluecone.app.controller.tenant;

import com.bluecone.app.dto.tenant.TenantBasicInfoUpdateRequest;
import com.bluecone.app.dto.tenant.TenantCreateRequest;
import com.bluecone.app.dto.tenant.TenantDetailResponse;
import com.bluecone.app.dto.tenant.TenantPlanChangeRequest;
import com.bluecone.app.dto.tenant.TenantPlatformBindRequest;
import com.bluecone.app.dto.tenant.TenantProfileUpdateRequest;
import com.bluecone.app.dto.tenant.TenantSummaryResponse;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户聚合 API，仅负责 DTO 转换与参数校验。
 */
@RestController
@RequestMapping("/api/tenants")
@Validated
@RequiredArgsConstructor
public class TenantController {

    private final TenantApplicationService tenantApplicationService;

    // 处理 POST /api/tenants 创建租户请求
    @PostMapping
    public Long createTenant(@Valid @RequestBody TenantCreateRequest request) {
        // 将校验通过的请求转换为命令并委派给应用服务
        return tenantApplicationService.createTenant(toCreateCommand(request));
    }

    @PutMapping("/{id}/basic")
    public void updateBasic(@PathVariable("id") Long tenantId,
                            @Valid @RequestBody TenantBasicInfoUpdateRequest request) {
        tenantApplicationService.updateTenantBasicInfo(new UpdateTenantBasicInfoCommand(
                tenantId,
                request.getTenantName(),
                request.getContactPerson(),
                request.getContactPhone(),
                request.getContactEmail(),
                request.getRemark(),
                request.getStatus(),
                request.getOperatorId()));
    }

    @PutMapping("/{id}/profile")
    public void updateProfile(@PathVariable("id") Long tenantId,
                              @Valid @RequestBody TenantProfileUpdateRequest request) {
        tenantApplicationService.updateTenantProfile(new UpdateTenantProfileCommand(
                tenantId,
                request.getTenantType(),
                request.getBusinessName(),
                request.getBusinessLicenseNo(),
                request.getBusinessLicenseUrl(),
                request.getLegalPersonName(),
                request.getLegalPersonIdNo(),
                request.getAddress(),
                request.getVerificationStatus(),
                request.getVerificationReason(),
                request.getOperatorId()));
    }

    @PostMapping("/{id}/platform")
    public void bindPlatform(@PathVariable("id") Long tenantId,
                             @Valid @RequestBody TenantPlatformBindRequest request) {
        tenantApplicationService.updateTenantPlatformAccount(new UpdateTenantPlatformAccountCommand(
                tenantId,
                request.getPlatformType(),
                request.getPlatformAccountId(),
                request.getAccountName(),
                request.getCredential(),
                request.getStatus(),
                request.getExpireAt(),
                request.getOperatorId()));
    }

    @PostMapping("/{id}/plan")
    public void changePlan(@PathVariable("id") Long tenantId,
                           @Valid @RequestBody TenantPlanChangeRequest request) {
        tenantApplicationService.changeTenantPlan(new ChangeTenantPlanCommand(
                tenantId,
                request.getPlanId(),
                request.getPayAmount(),
                request.getPayMethod(),
                request.getExpireAt(),
                request.getOperatorId()));
    }

    @GetMapping("/{id}")
    public TenantDetailResponse detail(@PathVariable("id") Long tenantId) {
        return toDetailResponse(tenantApplicationService.getTenantDetail(tenantId));
    }

    @GetMapping
    public List<TenantSummaryResponse> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "planId", required = false) Long planId,
            @RequestParam(value = "pageNo", defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "20") @Min(1) @Max(200) int pageSize) {
        List<TenantSummary> summaries = tenantApplicationService.listTenantSummary(
                new TenantQuery(keyword, status, planId, pageNo, pageSize));
        return summaries.stream().map(this::toSummaryResponse).toList();
    }

    private CreateTenantCommand toCreateCommand(TenantCreateRequest request) {
        return new CreateTenantCommand(
                // 租户名称
                request.getTenantName(),
                // 联系人姓名
                request.getContactPerson(),
                // 联系人电话
                request.getContactPhone(),
                // 联系人邮箱
                request.getContactEmail(),
                // 备注信息
                request.getRemark(),
                // 租户类型
                request.getTenantType(),
                // 主体名称
                request.getBusinessName(),
                // 营业执照编号
                request.getBusinessLicenseNo(),
                // 营业执照附件
                request.getBusinessLicenseUrl(),
                // 法人姓名
                request.getLegalPersonName(),
                // 法人证件号
                request.getLegalPersonIdNo(),
                // 主体地址
                request.getAddress(),
                // 操作人 ID
                request.getOperatorId(),
                // 首次订阅方案 ID
                request.getInitialPlanId(),
                // 方案到期时间
                request.getPlanExpireAt());
    }

    private TenantSummaryResponse toSummaryResponse(TenantSummary summary) {
        return TenantSummaryResponse.builder()
                .tenantId(summary.getTenantId())
                .tenantCode(summary.getTenantCode())
                .tenantName(summary.getTenantName())
                .status(summary.getStatus())
                .contactPerson(summary.getContactPerson())
                .contactPhone(summary.getContactPhone())
                .planId(summary.getPlanId())
                .planName(summary.getPlanName())
                .planExpireAt(summary.getPlanExpireAt())
                .createdAt(summary.getCreatedAt())
                .build();
    }

    private TenantDetailResponse toDetailResponse(TenantDetail detail) {
        TenantVerificationInfo verificationInfo = detail.getVerificationInfo();
        TenantPlanInfo planInfo = detail.getPlanInfo();
        List<TenantDetailResponse.PlatformAccountResponse> accounts = detail.getPlatformAccounts() == null
                ? List.of()
                : detail.getPlatformAccounts().stream().map(this::toPlatformResponse).toList();
        List<TenantDetailResponse.TenantMediaResponse> mediaList = detail.getMediaList() == null
                ? List.of()
                : detail.getMediaList().stream().map(this::toMediaResponse).toList();
        return TenantDetailResponse.builder()
                .tenantId(detail.getTenantId())
                .tenantCode(detail.getTenantCode())
                .tenantName(detail.getTenantName())
                .status(detail.getStatus())
                .contactPerson(detail.getContactPerson())
                .contactPhone(detail.getContactPhone())
                .contactEmail(detail.getContactEmail())
                .remark(detail.getRemark())
                .verification(verificationInfo == null ? null : TenantDetailResponse.TenantVerificationInfoResponse.builder()
                        .tenantType(verificationInfo.getTenantType())
                        .businessName(verificationInfo.getBusinessName())
                        .businessLicenseNo(verificationInfo.getBusinessLicenseNo())
                        .businessLicenseUrl(verificationInfo.getBusinessLicenseUrl())
                        .legalPersonName(verificationInfo.getLegalPersonName())
                        .legalPersonIdNo(verificationInfo.getLegalPersonIdNo())
                        .address(verificationInfo.getAddress())
                        .verificationStatus(verificationInfo.getVerificationStatus())
                        .verificationReason(verificationInfo.getVerificationReason())
                        .build())
                .plan(planInfo == null ? null : TenantDetailResponse.TenantPlanInfoResponse.builder()
                        .planId(planInfo.getPlanId())
                        .planName(planInfo.getPlanName())
                        .price(planInfo.getPrice())
                        .features(planInfo.getFeatures())
                        .status(planInfo.getStatus())
                        .expireAt(planInfo.getExpireAt())
                        .build())
                .platformAccounts(accounts)
                .mediaList(mediaList)
                .settings(detail.getSettings() != null ? detail.getSettings() : Map.of())
                .createdAt(detail.getCreatedAt())
                .updatedAt(detail.getUpdatedAt())
                .build();
    }

    private TenantDetailResponse.PlatformAccountResponse toPlatformResponse(TenantPlatformAccountView view) {
        return TenantDetailResponse.PlatformAccountResponse.builder()
                .id(view.getId())
                .platformType(view.getPlatformType())
                .platformAccountId(view.getPlatformAccountId())
                .accountName(view.getAccountName())
                .status(view.getStatus())
                .expireAt(view.getExpireAt())
                .build();
    }

    private TenantDetailResponse.TenantMediaResponse toMediaResponse(TenantMediaView media) {
        return TenantDetailResponse.TenantMediaResponse.builder()
                .id(media.getId())
                .mediaType(media.getMediaType())
                .url(media.getUrl())
                .description(media.getDescription())
                .createdAt(media.getCreatedAt())
                .build();
    }
}
