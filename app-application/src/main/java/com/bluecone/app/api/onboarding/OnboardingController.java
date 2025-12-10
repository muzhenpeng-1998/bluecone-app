package com.bluecone.app.api.onboarding;

import com.bluecone.app.api.ApiResponse;
import com.bluecone.app.api.onboarding.dto.AttachStoreRequest;
import com.bluecone.app.api.onboarding.dto.AttachTenantRequest;
import com.bluecone.app.api.onboarding.dto.BindAccountRequest;
import com.bluecone.app.api.onboarding.dto.StoreBasicInfoRequest;
import com.bluecone.app.api.onboarding.dto.StoreBasicInfoResponse;
import com.bluecone.app.api.onboarding.dto.StartSessionRequest;
import com.bluecone.app.api.onboarding.dto.StartSessionResponse;
import com.bluecone.app.api.onboarding.dto.TenantBasicInfoRequest;
import com.bluecone.app.api.onboarding.dto.TenantBasicInfoResponse;
import com.bluecone.app.api.onboarding.dto.WechatAuthUrlResponse;
import com.bluecone.app.api.onboarding.dto.WechatRegisterRequest;
import com.bluecone.app.api.onboarding.dto.WechatRegisterResponse;
import com.bluecone.app.core.error.BizErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.infra.tenant.dataobject.TenantOnboardingSessionDO;
import com.bluecone.app.tenant.application.onboarding.CreateStoreDraftCommand;
import com.bluecone.app.tenant.application.onboarding.CreateTenantDraftCommand;
import com.bluecone.app.tenant.application.onboarding.TenantOnboardingCreateService;
import com.bluecone.app.tenant.application.wechat.command.BuildAuthorizeUrlCommand;
import com.bluecone.app.tenant.application.wechat.CreateWechatRegisterTaskCommand;
import com.bluecone.app.tenant.application.wechat.WechatAuthorizationAppService;
import com.bluecone.app.tenant.application.wechat.WechatMiniProgramRegisterAppService;
import com.bluecone.app.tenant.service.TenantOnboardingAppService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户入驻 H5 流程控制器。
 *
 * 提供扫码进入、账号绑定、品牌信息回填、门店信息回填等入驻引导接口。
 */
@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final TenantOnboardingAppService tenantOnboardingAppService;
    private final TenantOnboardingCreateService tenantOnboardingCreateService;
    private final WechatMiniProgramRegisterAppService wechatMiniProgramRegisterAppService;
    private final WechatAuthorizationAppService wechatAuthorizationAppService;

    @Value("${wechat.open-platform.auth-redirect-uri:}")
    private String wechatAuthRedirectUri;

    public OnboardingController(TenantOnboardingAppService tenantOnboardingAppService,
                                TenantOnboardingCreateService tenantOnboardingCreateService,
                                WechatMiniProgramRegisterAppService wechatMiniProgramRegisterAppService,
                                WechatAuthorizationAppService wechatAuthorizationAppService) {
        this.tenantOnboardingAppService = tenantOnboardingAppService;
        this.tenantOnboardingCreateService = tenantOnboardingCreateService;
        this.wechatMiniProgramRegisterAppService = wechatMiniProgramRegisterAppService;
        this.wechatAuthorizationAppService = wechatAuthorizationAppService;
    }

    /**
     * 创建或获取入驻会话。
     *
     * 扫码进入 H5 时调用，生成一个新的 sessionToken 并返回前端。
     */
    @PostMapping("/session/start")
    public ApiResponse<StartSessionResponse> startSession(@RequestBody(required = false) StartSessionRequest request) {
        String channelCode = request != null ? request.getChannelCode() : null;
        String sessionToken = tenantOnboardingAppService.startSession(channelCode);
        StartSessionResponse response = new StartSessionResponse();
        response.setSessionToken(sessionToken);
        return ApiResponse.success(response);
    }

    /**
     * 绑定平台用户信息。
     *
     * 手机号登录后调用，将 userId 与联系手机号绑定到入驻会话。
     */
    @PostMapping("/account/bind")
    public ApiResponse<Void> bindAccount(@RequestBody BindAccountRequest request) {
        tenantOnboardingAppService.attachUser(request.getSessionToken(), request.getUserId(), request.getContactPhone());
        return ApiResponse.success();
    }

    /**
     * 在填写品牌信息后，回填租户 ID。
     *
     * 仅负责把 tenantId 绑定进会话，租户创建逻辑在其他应用服务中完成。
     */
    @PostMapping("/tenant/attach")
    public ApiResponse<Void> attachTenant(@RequestBody AttachTenantRequest request) {
        tenantOnboardingAppService.attachTenant(request.getSessionToken(), request.getTenantId());
        return ApiResponse.success();
    }

    /**
     * 在填写门店信息后，回填门店 ID。
     *
     * 仅负责把 storeId 绑定进会话，门店创建逻辑在其他应用服务中完成。
     */
    @PostMapping("/store/attach")
    public ApiResponse<Void> attachStore(@RequestBody AttachStoreRequest request) {
        tenantOnboardingAppService.attachStore(request.getSessionToken(), request.getStoreId());
        return ApiResponse.success();
    }

    /**
     * 创建租户草稿基础信息，并回填到入驻会话。
     *
     * 仅用于 H5 入驻引导流程。
     */
    @PostMapping("/tenant/basic-info")
    public ApiResponse<TenantBasicInfoResponse> createTenantBasicInfo(
            @RequestBody TenantBasicInfoRequest request) {
        TenantOnboardingSessionDO session = tenantOnboardingAppService.findBySessionToken(request.getSessionToken());
        if (session == null) {
            throw new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "入驻会话不存在或已失效");
        }
        if (session.getUserId() == null) {
            throw new BizException(BizErrorCode.INVALID_PARAM, "入驻会话尚未绑定用户");
        }

        String sourceChannel = request.getSourceChannel() != null
                ? request.getSourceChannel()
                : session.getChannelCode();
        String contactPhone = session.getContactPhone();

        CreateTenantDraftCommand command = new CreateTenantDraftCommand(
                request.getTenantName(),
                request.getLegalName(),
                request.getBusinessCategory(),
                sourceChannel,
                session.getUserId(),
                contactPhone
        );

        Long tenantId = tenantOnboardingCreateService.createTenantDraftForOnboarding(command);
        tenantOnboardingAppService.attachTenant(request.getSessionToken(), tenantId);

        TenantBasicInfoResponse response = new TenantBasicInfoResponse();
        response.setTenantId(tenantId);
        return ApiResponse.success(response);
    }

    /**
     * 创建首店草稿基础信息，并回填到入驻会话。
     *
     * 仅用于 H5 入驻引导流程。
     */
    @PostMapping("/store/basic-info")
    public ApiResponse<StoreBasicInfoResponse> createStoreBasicInfo(
            @RequestBody StoreBasicInfoRequest request) {
        TenantOnboardingSessionDO session = tenantOnboardingAppService.findBySessionToken(request.getSessionToken());
        if (session == null) {
            throw new BizException(BizErrorCode.RESOURCE_NOT_FOUND, "入驻会话不存在或已失效");
        }
        if (session.getTenantId() == null) {
            throw new BizException(BizErrorCode.INVALID_PARAM, "入驻会话尚未绑定租户");
        }

        String contactPhone = request.getContactPhone() != null
                ? request.getContactPhone()
                : session.getContactPhone();

        CreateStoreDraftCommand command = new CreateStoreDraftCommand(
                session.getTenantId(),
                request.getStoreName(),
                request.getCity(),
                request.getDistrict(),
                request.getAddress(),
                request.getBizScene(),
                contactPhone
        );

        Long storeId = tenantOnboardingCreateService.createStoreDraftForOnboarding(command);
        tenantOnboardingAppService.attachStore(request.getSessionToken(), storeId);

        StoreBasicInfoResponse response = new StoreBasicInfoResponse();
        response.setStoreId(storeId);
        return ApiResponse.success(response);
    }

    /**
     * 获取微信开放平台授权 URL（已有小程序授权绑定）。
     * <p>
     * 前端在入驻 H5 场景下调用本接口，根据返回的授权链接跳转到微信授权页。
     * </p>
     */
    @GetMapping("/wechat/auth-url")
    public ApiResponse<WechatAuthUrlResponse> getWechatAuthorizeUrl(@RequestParam("sessionToken") String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new BizException(BizErrorCode.INVALID_PARAM, "sessionToken 不能为空");
        }
        if (wechatAuthRedirectUri == null || wechatAuthRedirectUri.isBlank()) {
            throw new BizException(BizErrorCode.INVALID_PARAM, "微信授权回调地址未配置");
        }

        BuildAuthorizeUrlCommand cmd = new BuildAuthorizeUrlCommand(sessionToken, wechatAuthRedirectUri);
        String authorizeUrl = wechatAuthorizationAppService.buildAuthorizeUrlForOnboarding(cmd);

        WechatAuthUrlResponse response = new WechatAuthUrlResponse();
        response.setAuthorizeUrl(authorizeUrl);
        return ApiResponse.success(response);
    }

    /**
     * 一键开通微信小程序：创建注册任务记录。
     *
     * 仅负责在 bc_wechat_register_task 中落任务记录，不直接调用微信开放平台 API。
     */
    @PostMapping("/wechat/register")
    public ApiResponse<WechatRegisterResponse> registerMiniProgram(@RequestBody WechatRegisterRequest request) {
        if (request.getSessionToken() == null || request.getSessionToken().isBlank()) {
            throw new BizException(BizErrorCode.INVALID_PARAM, "sessionToken 不能为空");
        }
        if (request.getRegisterType() == null || request.getRegisterType().isBlank()) {
            throw new BizException(BizErrorCode.INVALID_PARAM, "registerType 不能为空");
        }

        String type = request.getRegisterType().toUpperCase();
        if ("FORMAL".equals(type)) {
            // TODO: 可接入更完备的参数校验框架（如 @Valid）
            if (request.getCompanyName() == null || request.getCompanyName().isBlank()
                    || request.getCompanyCode() == null || request.getCompanyCode().isBlank()
                    || request.getCompanyCodeType() == null
                    || request.getLegalPersonaWechat() == null || request.getLegalPersonaWechat().isBlank()
                    || request.getLegalPersonaName() == null || request.getLegalPersonaName().isBlank()) {
                throw new BizException(BizErrorCode.INVALID_PARAM, "FORMAL 注册需要完整的企业主体信息");
            }
        } else if ("TRIAL".equals(type)) {
            if (request.getTrialOpenId() == null || request.getTrialOpenId().isBlank()) {
                throw new BizException(BizErrorCode.INVALID_PARAM, "TRIAL 注册需要提供联系人 openid");
            }
        } else {
            throw new BizException(BizErrorCode.INVALID_PARAM, "registerType 仅支持 FORMAL 或 TRIAL");
        }

        CreateWechatRegisterTaskCommand command = new CreateWechatRegisterTaskCommand(
                request.getSessionToken(),
                type,
                request.getRequestPayloadJson(),
                request.getCompanyName(),
                request.getCompanyCode(),
                request.getCompanyCodeType(),
                request.getLegalPersonaWechat(),
                request.getLegalPersonaName(),
                request.getTrialMiniProgramName(),
                request.getTrialOpenId()
        );

        Long taskId = wechatMiniProgramRegisterAppService.createRegisterTask(command);

        WechatRegisterResponse response = new WechatRegisterResponse();
        response.setTaskId(taskId);

        // TODO 后续在此处调用微信开放平台 fastregisterweapp / fastregisterbetaweapp，并更新任务状态

        return ApiResponse.success(response);
    }
}
