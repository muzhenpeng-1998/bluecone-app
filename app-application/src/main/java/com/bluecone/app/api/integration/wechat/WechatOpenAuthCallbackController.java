package com.bluecone.app.application.wechat;

import com.bluecone.app.api.advice.NoApiResponseWrap;
import com.bluecone.app.infra.wechat.openplatform.AuthorizerInfoResult;
import com.bluecone.app.infra.wechat.openplatform.QueryAuthResult;
import com.bluecone.app.infra.wechat.openplatform.WeChatOpenPlatformClient;
import com.bluecone.app.infra.wechat.openplatform.WechatComponentCredentialService;
import com.bluecone.app.tenant.application.wechat.command.WechatAuthorizedEventCommand;
import com.bluecone.app.tenant.application.wechat.WechatOpenCallbackAppService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信开放平台授权完成后的浏览器回调入口。
 * <p>
 * 用于接收 auth_code，将其兑换为授权信息，并写入本地授权表，
 * 供入驻 H5 流程使用。
 * </p>
 */
@Tag(name = "🔌 第三方集成 > 微信相关 > 微信开放平台回调", description = "微信授权回调接口")
@RestController
@RequestMapping("/api/wechat/open")
@NoApiResponseWrap
public class WechatOpenAuthCallbackController {

    private static final Logger log = LoggerFactory.getLogger(WechatOpenAuthCallbackController.class);

    private final WeChatOpenPlatformClient weChatOpenPlatformClient;
    private final WechatOpenCallbackAppService wechatOpenCallbackAppService;
    private final WechatComponentCredentialService wechatComponentCredentialService;

    public WechatOpenAuthCallbackController(WeChatOpenPlatformClient weChatOpenPlatformClient,
                                            WechatOpenCallbackAppService wechatOpenCallbackAppService,
                                            WechatComponentCredentialService wechatComponentCredentialService) {
        this.weChatOpenPlatformClient = weChatOpenPlatformClient;
        this.wechatOpenCallbackAppService = wechatOpenCallbackAppService;
        this.wechatComponentCredentialService = wechatComponentCredentialService;
    }

    /**
     * 微信开放平台授权完成后的回调。
     * <p>
     * 前端浏览器在授权页完成授权后会重定向到此接口，带上 auth_code 和之前拼接的 sessionToken。
     * 当前实现：
     * 1) 使用 auth_code 调用 queryAuth 获取授权信息；
     * 2) 调用 getAuthorizerInfo 获取小程序基本信息；
     * 3) 组装授权事件命令交给应用服务处理；
     * 4) 返回简单提示，后续可改为重定向到入驻 H5 成功页。
     * </p>
     */
    @GetMapping("/auth/callback")
    public String handleAuthCallback(
            @RequestParam("auth_code") String authCode,
            @RequestParam(value = "expires_in", required = false) String expiresIn,
            @RequestParam(value = "sessionToken", required = false) String sessionToken
    ) {
        log.info("[WechatOpenAuth] auth callback received, authCode={}, expiresIn={}, sessionToken={}",
                authCode, expiresIn, sessionToken);

        String componentAccessToken = wechatComponentCredentialService.getValidComponentAccessToken();

        QueryAuthResult authResult = weChatOpenPlatformClient.queryAuth(componentAccessToken, authCode);
        if (authResult == null || !authResult.isSuccess() || authResult.getAuthorizerAppId() == null) {
            log.error("[WechatOpenAuth] queryAuth failed, authCode={}, errcode={}, errmsg={}",
                    authCode,
                    authResult != null ? authResult.getErrcode() : null,
                    authResult != null ? authResult.getErrmsg() : null);
            return "授权失败，请稍后重试";
        }

        String authorizerAppId = authResult.getAuthorizerAppId();
        Optional<AuthorizerInfoResult> infoOpt =
                weChatOpenPlatformClient.getAuthorizerInfo(componentAccessToken, authorizerAppId);
        AuthorizerInfoResult info = infoOpt.orElse(null);

        WechatAuthorizedEventCommand cmd = new WechatAuthorizedEventCommand(
                authorizerAppId,
                authResult.getAuthorizerRefreshToken(),
                info != null ? info.getNickName() : null,
                info != null ? info.getHeadImg() : null,
                info != null ? info.getPrincipalType() : null,
                info != null ? info.getPrincipalName() : null,
                info != null ? info.getSignature() : null,
                null,
                info != null ? info.getVerifyType() : null,
                null,
                null,
                null
        );

        wechatOpenCallbackAppService.handleMiniProgramAuthorized(cmd);

        // TODO: 后续可改为重定向到入驻 H5 成功页，例如带上 sessionToken 的 URL
        return "授权成功，请回到小程序开店页面刷新状态";
    }
}
