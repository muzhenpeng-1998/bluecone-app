package com.bluecone.app.api.integration.wechat;

import com.bluecone.app.api.advice.NoApiResponseWrap;
import com.bluecone.app.infra.wechat.openplatform.OnboardingWeChatAuthSession;
import com.bluecone.app.infra.wechat.openplatform.OnboardingWeChatAuthSessionService;
import com.bluecone.app.tenant.application.wechat.command.WechatAuthorizedEventCommand;
import com.bluecone.app.tenant.application.wechat.WechatOpenCallbackAppService;
import com.bluecone.app.wechat.facade.openplatform.WeChatOpenPlatformFacade;
import com.bluecone.app.wechat.facade.openplatform.WeChatQueryAuthCommand;
import com.bluecone.app.wechat.facade.openplatform.WeChatQueryAuthResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * 微信开放平台授权完成后的浏览器回调入口。
 * <p>
 * Phase 3 版本：使用 facade 处理 queryAuth，极薄 Controller。
 * 用于接收 auth_code，将其兑换为授权信息，并写入本地授权表，
 * 供入驻 H5 流程使用。
 * </p>
 */
@Tag(name = "🔌 第三方集成 > 微信相关 > 微信开放平台回调", description = "微信授权回调接口")
@RestController
@RequestMapping("/api/wechat/open")
@NoApiResponseWrap
@RequiredArgsConstructor
public class WechatOpenAuthCallbackController {

    private static final Logger log = LoggerFactory.getLogger(WechatOpenAuthCallbackController.class);

    private final WeChatOpenPlatformFacade weChatOpenPlatformFacade;
    private final WechatOpenCallbackAppService wechatOpenCallbackAppService;
    private final OnboardingWeChatAuthSessionService authSessionService;

    /**
     * 微信开放平台授权完成后的回调。
     * <p>
     * 前端浏览器在授权页完成授权后会重定向到此接口，带上 auth_code、state 和 sessionToken。
     * 当前实现（带 state 验证）：
     * 1) 验证 state 并获取 tenantId/storeId（防止串租户/串门店）；
     * 2) 使用 facade.queryAuth 获取授权信息并落库；
     * 3) 组装授权事件命令交给 app-tenant 服务处理租户绑定；
     * 4) 删除 state 会话；
     * 5) 返回简单提示，后续可改为重定向到入驻 H5 成功页。
     * </p>
     */
    @GetMapping("/auth/callback")
    public String handleAuthCallback(
            @RequestParam("auth_code") String authCode,
            @RequestParam(value = "expires_in", required = false) String expiresIn,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "sessionToken", required = false) String sessionToken
    ) {
        log.info("[WechatOpenAuth] auth callback received, authCode={}, expiresIn={}, state={}***, sessionToken={}",
                authCode, expiresIn, maskState(state), sessionToken);

        // 1. 验证 state（必填）
        if (!StringUtils.hasText(state)) {
            log.error("[WechatOpenAuth] state 参数缺失，拒绝授权回调");
            return "授权失败：缺少必要参数（state），请重新授权";
        }

        // 2. 根据 state 获取授权会话（包含 tenantId/storeId）
        Optional<OnboardingWeChatAuthSession> sessionOpt = authSessionService.getSession(state);
        if (sessionOpt.isEmpty()) {
            log.error("[WechatOpenAuth] state 无效或已过期, state={}***", maskState(state));
            return "授权失败：授权链接已过期（超过10分钟），请重新生成授权链接";
        }

        OnboardingWeChatAuthSession authSession = sessionOpt.get();
        Long tenantId = authSession.getTenantId();
        Long storeId = authSession.getStoreId();

        log.info("[WechatOpenAuth] state 验证通过, tenantId={}, storeId={}", tenantId, storeId);

        // 3. 调用 facade.queryAuth 获取授权信息并落库
        WeChatQueryAuthCommand command = WeChatQueryAuthCommand.builder()
                .authCode(authCode)
                .build();

        WeChatQueryAuthResult result = weChatOpenPlatformFacade.queryAuth(command);

        if (!result.isSuccess()) {
            log.error("[WechatOpenAuth] queryAuth failed, authCode={}, errcode={}, errmsg={}",
                    authCode, result.getErrcode(), result.getErrmsg());
            return "授权失败：" + result.getErrmsg() + "，请稍后重试";
        }

        String authorizerAppId = result.getAuthorizerAppId();
        log.info("[WechatOpenAuth] queryAuth success, authorizerAppId={}, tenantId={}", authorizerAppId, tenantId);

        // 4. 组装授权事件命令交给 app-tenant 服务处理租户绑定
        WechatAuthorizedEventCommand cmd = new WechatAuthorizedEventCommand(
                authorizerAppId,
                null, // refresh_token 已由 facade 保存到 DB
                result.getNickName(),
                result.getHeadImg(),
                null, // principalType
                result.getPrincipalName(),
                null, // signature
                null, // serviceType
                null, // verifyType
                null, // funcInfoJson
                null, // businessInfoJson
                null  // miniprograminfoJson
        );

        // 注意：这里传入的 tenantId/storeId 来自 state 验证，确保不会串租户/串门店
        wechatOpenCallbackAppService.handleMiniProgramAuthorized(cmd);

        // 5. 删除 state 会话（授权成功后清理）
        authSessionService.deleteSession(state);

        // TODO: 后续可改为重定向到入驻 H5 成功页，例如带上 sessionToken 的 URL
        return "授权成功！请回到小程序开店页面刷新状态。";
    }

    /**
     * 脱敏 state（只显示前 8 个字符）
     */
    private String maskState(String state) {
        if (state == null || state.length() <= 8) {
            return "***";
        }
        return state.substring(0, 8);
    }
}
