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
 * 租户管理内部控制器
 * 
 * <p>提供租户的完整生命周期管理功能，作为租户聚合根的API入口。
 * 
 * <p><b>职责范围：</b>
 * <ul>
 *   <li>DTO与Command/Query对象的转换</li>
 *   <li>请求参数的格式校验（通过@Valid注解）</li>
 *   <li>将业务逻辑委派给应用服务层</li>
 * </ul>
 * 
 * <p><b>架构说明：</b>
 * <ul>
 *   <li>Controller层：只做参数校验和对象转换，不包含业务逻辑</li>
 *   <li>ApplicationService层：编排领域服务和聚合根，处理事务边界</li>
 *   <li>Domain层：包含核心业务规则和领域模型</li>
 * </ul>
 * 
 * <p><b>多租户隔离：</b>
 * 租户上下文由中间件自动注入，所有查询和操作都基于当前租户。
 * 
 * @author BlueCone
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/tenants")
@Validated
@RequiredArgsConstructor
public class TenantInternalController {

    /**
     * 租户应用服务
     * 负责编排租户相关的业务流程
     */
    private final TenantApplicationService tenantApplicationService;

    /**
     * 创建新租户
     * 
     * <p>创建一个新的租户账号，包括基本信息、认证信息和初始订阅方案。
     * 
     * <p><b>创建流程：</b>
     * <ol>
     *   <li>生成唯一的租户编码（tenantCode）</li>
     *   <li>保存租户基本信息和联系方式</li>
     *   <li>保存租户认证信息（营业执照等）</li>
     *   <li>绑定初始订阅方案</li>
     *   <li>初始化租户配置和权限</li>
     * </ol>
     * 
     * <p><b>业务规则：</b>
     * <ul>
     *   <li>租户名称在系统内必须唯一</li>
     *   <li>企业类型租户必须提供营业执照信息</li>
     *   <li>联系电话和邮箱至少提供一个</li>
     *   <li>必须指定初始订阅方案</li>
     * </ul>
     * 
     * @param request 租户创建请求，包含基本信息、认证信息和方案ID
     * @return 新创建的租户ID
     */
    @PostMapping
    public Long createTenant(@Valid @RequestBody TenantCreateRequest request) {
        // 将校验通过的DTO转换为领域命令对象
        return tenantApplicationService.createTenant(toCreateCommand(request));
    }

    /**
     * 更新租户基本信息
     * 
     * <p>更新租户的基本信息，包括名称、联系方式、状态等。
     * 
     * <p><b>可更新字段：</b>
     * <ul>
     *   <li>租户名称</li>
     *   <li>联系人姓名</li>
     *   <li>联系电话</li>
     *   <li>联系邮箱</li>
     *   <li>备注信息</li>
     *   <li>租户状态（启用/禁用）</li>
     * </ul>
     * 
     * <p><b>业务规则：</b>
     * <ul>
     *   <li>租户名称修改后仍需保持系统内唯一</li>
     *   <li>禁用租户会导致其下所有用户无法登录</li>
     *   <li>更新操作会记录操作人ID用于审计</li>
     * </ul>
     * 
     * @param tenantId 租户ID，从URL路径中获取
     * @param request 基本信息更新请求
     */
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

    /**
     * 更新租户认证信息
     * 
     * <p>更新租户的企业认证信息，用于实名认证和资质审核。
     * 
     * <p><b>可更新字段：</b>
     * <ul>
     *   <li>租户类型（个人/企业）</li>
     *   <li>企业名称</li>
     *   <li>营业执照编号</li>
     *   <li>营业执照附件URL</li>
     *   <li>法人代表姓名</li>
     *   <li>法人身份证号</li>
     *   <li>企业地址</li>
     *   <li>认证状态（待审核/已通过/已拒绝）</li>
     *   <li>认证拒绝原因</li>
     * </ul>
     * 
     * <p><b>业务规则：</b>
     * <ul>
     *   <li>企业类型租户必须完整填写所有企业信息</li>
     *   <li>认证状态变更会触发通知事件</li>
     *   <li>认证通过后，某些字段不允许修改（如营业执照号）</li>
     * </ul>
     * 
     * @param tenantId 租户ID，从URL路径中获取
     * @param request 认证信息更新请求
     */
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

    /**
     * 绑定或更新第三方平台账号
     * 
     * <p>将租户与第三方平台账号进行绑定，用于集成微信公众号、支付宝等平台服务。
     * 
     * <p><b>支持的平台类型：</b>
     * <ul>
     *   <li>微信公众号</li>
     *   <li>微信小程序</li>
     *   <li>支付宝生活号</li>
     *   <li>抖音小程序</li>
     * </ul>
     * 
     * <p><b>绑定流程：</b>
     * <ol>
     *   <li>验证平台账号的有效性</li>
     *   <li>保存平台账号ID和凭证信息</li>
     *   <li>设置账号状态和过期时间</li>
     *   <li>触发平台配置同步事件</li>
     * </ol>
     * 
     * <p><b>安全性说明：</b>
     * <ul>
     *   <li>平台凭证（如AppSecret）会加密存储</li>
     *   <li>凭证过期前会发送续期提醒</li>
     *   <li>同一平台类型只能绑定一个账号</li>
     * </ul>
     * 
     * @param tenantId 租户ID，从URL路径中获取
     * @param request 平台账号绑定请求
     */
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

    /**
     * 变更租户订阅方案
     * 
     * <p>为租户变更订阅方案，支持升级、降级和续费操作。
     * 
     * <p><b>方案变更类型：</b>
     * <ul>
     *   <li>升级：从低级方案升级到高级方案，立即生效</li>
     *   <li>降级：从高级方案降级到低级方案，下个周期生效</li>
     *   <li>续费：延长当前方案的有效期</li>
     * </ul>
     * 
     * <p><b>计费规则：</b>
     * <ul>
     *   <li>升级时，按剩余天数计算差价</li>
     *   <li>降级时，差价退回到租户钱包</li>
     *   <li>续费时，按新方案价格全额收费</li>
     * </ul>
     * 
     * <p><b>变更流程：</b>
     * <ol>
     *   <li>验证目标方案的有效性</li>
     *   <li>计算费用并处理支付</li>
     *   <li>更新方案信息和到期时间</li>
     *   <li>调整功能权限和配额</li>
     *   <li>发送变更通知</li>
     * </ol>
     * 
     * @param tenantId 租户ID，从URL路径中获取
     * @param request 方案变更请求，包含目标方案ID、支付金额等
     */
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

    /**
     * 查询租户详细信息
     * 
     * <p>获取租户的完整信息，包括基本信息、认证信息、订阅方案、平台绑定等。
     * 
     * <p><b>返回信息包含：</b>
     * <ul>
     *   <li>基本信息：ID、编码、名称、状态、联系方式</li>
     *   <li>认证信息：企业类型、营业执照、法人信息、认证状态</li>
     *   <li>方案信息：当前方案、价格、功能列表、到期时间</li>
     *   <li>平台账号：已绑定的第三方平台列表</li>
     *   <li>媒体资源：Logo、宣传图等</li>
     *   <li>自定义配置：租户级别的配置项</li>
     * </ul>
     * 
     * @param tenantId 租户ID，从URL路径中获取
     * @return 租户详细信息响应对象
     */
    @GetMapping("/{id}")
    public TenantDetailResponse detail(@PathVariable("id") Long tenantId) {
        return toDetailResponse(tenantApplicationService.getTenantDetail(tenantId));
    }

    /**
     * 分页查询租户列表
     * 
     * <p>查询租户摘要信息列表，支持多条件筛选和分页。
     * 
     * <p><b>查询条件：</b>
     * <ul>
     *   <li>keyword：关键词搜索，匹配租户名称、编码、联系人</li>
     *   <li>status：租户状态筛选（启用/禁用）</li>
     *   <li>planId：按订阅方案筛选</li>
     * </ul>
     * 
     * <p><b>分页参数：</b>
     * <ul>
     *   <li>pageNo：页码，从1开始，默认1</li>
     *   <li>pageSize：每页大小，默认20，最大200</li>
     * </ul>
     * 
     * <p><b>返回字段：</b>
     * 每条记录包含租户ID、编码、名称、状态、联系方式、方案信息、创建时间等摘要信息。
     * 
     * @param keyword 关键词（可选）
     * @param status 状态（可选）
     * @param planId 方案ID（可选）
     * @param pageNo 页码，默认1
     * @param pageSize 每页大小，默认20
     * @return 租户摘要信息列表
     */
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

    /**
     * 将创建请求DTO转换为领域命令对象
     * 
     * <p>这是Controller层到Application层的适配转换，
     * 将外部的HTTP请求对象转换为内部的领域命令对象。
     * 
     * @param request 租户创建请求DTO
     * @return 创建租户命令对象
     */
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
                // 租户类型（个人/企业）
                request.getTenantType(),
                // 企业主体名称
                request.getBusinessName(),
                // 营业执照编号
                request.getBusinessLicenseNo(),
                // 营业执照附件URL
                request.getBusinessLicenseUrl(),
                // 法人代表姓名
                request.getLegalPersonName(),
                // 法人身份证号
                request.getLegalPersonIdNo(),
                // 企业注册地址
                request.getAddress(),
                // 操作人ID（用于审计）
                request.getOperatorId(),
                // 首次订阅方案ID
                request.getInitialPlanId(),
                // 方案到期时间
                request.getPlanExpireAt());
    }

    /**
     * 将领域模型转换为摘要响应DTO
     * 
     * <p>将内部的领域模型对象转换为外部的HTTP响应对象，
     * 只包含列表展示所需的摘要信息。
     * 
     * @param summary 租户摘要领域模型
     * @return 租户摘要响应DTO
     */
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

    /**
     * 将领域模型转换为详情响应DTO
     * 
     * <p>将内部的领域模型对象转换为外部的HTTP响应对象，
     * 包含租户的完整详细信息。
     * 
     * <p><b>转换内容包括：</b>
     * <ul>
     *   <li>基本信息</li>
     *   <li>认证信息（如果存在）</li>
     *   <li>订阅方案信息（如果存在）</li>
     *   <li>平台账号列表</li>
     *   <li>媒体资源列表</li>
     *   <li>自定义配置</li>
     * </ul>
     * 
     * @param detail 租户详情领域模型
     * @return 租户详情响应DTO
     */
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

    /**
     * 将平台账号视图对象转换为响应DTO
     * 
     * @param view 平台账号视图对象
     * @return 平台账号响应DTO
     */
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

    /**
     * 将媒体资源视图对象转换为响应DTO
     * 
     * @param media 媒体资源视图对象
     * @return 媒体资源响应DTO
     */
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
