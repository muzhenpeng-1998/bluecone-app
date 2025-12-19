package com.bluecone.app.tenant.application.wechat;

import com.bluecone.app.core.error.BizErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.infra.tenant.dataobject.TenantOnboardingSessionDO;
import com.bluecone.app.infra.wechat.openplatform.PreAuthCodeResult;
import com.bluecone.app.infra.wechat.openplatform.WeChatOpenPlatformClient;
import com.bluecone.app.infra.wechat.openplatform.WechatComponentCredentialService;
import com.bluecone.app.tenant.application.wechat.command.BuildAuthorizeUrlCommand;
import com.bluecone.app.tenant.service.TenantOnboardingAppService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 微信开放平台授权 URL 构建服务。
 * <p>
 * 用于入驻 H5 场景下生成微信开放平台授权链接，商家点击后跳转至微信授权页，
 * 将已有小程序授权绑定到当前租户/门店。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class WechatAuthorizationAppService {

    private static final Logger log = LoggerFactory.getLogger(WechatAuthorizationAppService.class);

    private final TenantOnboardingAppService tenantOnboardingAppService;
    private final WeChatOpenPlatformClient weChatOpenPlatformClient;
    private final WechatComponentCredentialService wechatComponentCredentialService;

    @Value("${wechat.open-platform.component-app-id:}")
    private String componentAppId;

    /**
     * 为入驻 H5 场景构建微信开放平台授权 URL。
     * <p>
     * 1) 校验并加载入驻会话；
     * 2) 获取预授权码 pre_auth_code；
     * 3) 在 redirectUri 上追加 sessionToken 作为回调参数；
     * 4) 拼接 componentloginpage 授权地址。
     * </p>
     *
     * @param command 构建授权 URL 的命令
     * @return 可直接用于前端跳转的授权 URL
     */
    @Transactional(readOnly = true)
    public String buildAuthorizeUrlForOnboarding(BuildAuthorizeUrlCommand command) {
        if (!StringUtils.hasText(command.sessionToken())) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "sessionToken 不能为空");
        }
        if (!StringUtils.hasText(command.redirectUri())) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "redirectUri 不能为空");
        }

        TenantOnboardingSessionDO session = tenantOnboardingAppService.findBySessionToken(command.sessionToken());
        if (session == null) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "入驻会话不存在或已失效");
        }
        if (session.getTenantId() == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "入驻会话尚未绑定租户，无法生成授权链接");
        }

        if (!StringUtils.hasText(componentAppId)) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "微信开放平台 componentAppId 未配置");
        }

        String componentAccessToken = wechatComponentCredentialService.getValidComponentAccessToken();

        PreAuthCodeResult preAuth = weChatOpenPlatformClient.createPreAuthCode(componentAccessToken);
        if (preAuth == null || !preAuth.isSuccess() || !StringUtils.hasText(preAuth.getPreAuthCode())) {
            log.error("[WechatOpenAuth] createPreAuthCode failed, tenantId={}, sessionToken={}, errcode={}, errmsg={}",
                    session.getTenantId(), command.sessionToken(),
                    preAuth != null ? preAuth.getErrcode() : null,
                    preAuth != null ? preAuth.getErrmsg() : null);
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "获取微信预授权码失败");
        }

        String redirectUriWithSession = appendSessionToken(command.redirectUri(), command.sessionToken());
        String encodedRedirectUri = urlEncode(redirectUriWithSession);

        StringBuilder url = new StringBuilder("https://mp.weixin.qq.com/cgi-bin/componentloginpage");
        url.append("?component_appid=").append(urlEncode(componentAppId));
        url.append("&pre_auth_code=").append(urlEncode(preAuth.getPreAuthCode()));
        url.append("&redirect_uri=").append(encodedRedirectUri);

        String authorizeUrl = url.toString();
        log.info("[WechatOpenAuth] built authorize url, tenantId={}, sessionToken={}, url={}",
                session.getTenantId(), command.sessionToken(), authorizeUrl);
        return authorizeUrl;
    }

    private String appendSessionToken(String redirectUri, String sessionToken) {
        StringBuilder sb = new StringBuilder(redirectUri);
        if (redirectUri.contains("?")) {
            sb.append("&");
        } else {
            sb.append("?");
        }
        sb.append("sessionToken=").append(urlEncode(sessionToken));
        return sb.toString();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
