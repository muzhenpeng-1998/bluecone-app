package com.bluecone.app.api.integration.wechat;

import com.bluecone.app.api.advice.NoApiResponseWrap;
import com.bluecone.app.tenant.application.wechat.command.WechatAuthorizedEventCommand;
import com.bluecone.app.tenant.application.wechat.WechatOpenCallbackAppService;
import com.bluecone.app.wechat.facade.openplatform.WeChatOpenPlatformFacade;
import com.bluecone.app.wechat.facade.openplatform.WeChatQueryAuthCommand;
import com.bluecone.app.wechat.facade.openplatform.WeChatQueryAuthResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * 微信开放平台授权完成后的回调。
     * <p>
     * 前端浏览器在授权页完成授权后会重定向到此接口，带上 auth_code 和之前拼接的 sessionToken。
     * 当前实现：
     * 1) 使用 facade.queryAuth 获取授权信息并落库；
     * 2) 组装授权事件命令交给 app-tenant 服务处理租户绑定；
     * 3) 返回简单提示，后续可改为重定向到入驻 H5 成功页。
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

        // 1. 调用 facade.queryAuth 获取授权信息并落库
        WeChatQueryAuthCommand command = WeChatQueryAuthCommand.builder()
                .authCode(authCode)
                .build();

        WeChatQueryAuthResult result = weChatOpenPlatformFacade.queryAuth(command);

        if (!result.isSuccess()) {
            log.error("[WechatOpenAuth] queryAuth failed, authCode={}, errcode={}, errmsg={}",
                    authCode, result.getErrcode(), result.getErrmsg());
            return "授权失败，请稍后重试";
        }

        String authorizerAppId = result.getAuthorizerAppId();
        log.info("[WechatOpenAuth] queryAuth success, authorizerAppId={}", authorizerAppId);

        // 2. 组装授权事件命令交给 app-tenant 服务处理租户绑定
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

        // TODO: 传入 sessionToken，让应用服务根据 sessionToken 查询 tenantId
        // 当前保持原有调用方式，后续可以扩展 handleMiniProgramAuthorized 方法支持 sessionToken
        wechatOpenCallbackAppService.handleMiniProgramAuthorized(cmd);

        // TODO: 后续可改为重定向到入驻 H5 成功页，例如带上 sessionToken 的 URL
        return "授权成功，请回到小程序开店页面刷新状态";
    }
}
