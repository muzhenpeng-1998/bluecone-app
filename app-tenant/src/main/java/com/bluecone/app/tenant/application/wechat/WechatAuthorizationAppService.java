package com.bluecone.app.tenant.application.wechat;

import com.bluecone.app.core.error.BizErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.infra.tenant.dataobject.TenantOnboardingSessionDO;
import com.bluecone.app.wechat.facade.openplatform.*;
import com.bluecone.app.tenant.application.wechat.command.BuildAuthorizeUrlCommand;
import com.bluecone.app.tenant.service.TenantOnboardingAppService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 微信开放平台授权 URL 构建服务。
 * <p>
 * Phase 3 版本：使用 WeChatOpenPlatformFacade，不直接依赖 WxJava SDK。
 * 用于入驻 H5 场景下生成微信开放平台授权链接，商家点击后跳转至微信授权页，
 * 将已有小程序授权绑定到当前租户/门店。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class WechatAuthorizationAppService {

    private static final Logger log = LoggerFactory.getLogger(WechatAuthorizationAppService.class);

    private final TenantOnboardingAppService tenantOnboardingAppService;
    private final WeChatOpenPlatformFacade weChatOpenPlatformFacade;

    /**
     * 为入驻 H5 场景构建微信开放平台授权 URL。
     * <p>
     * Phase 3 版本：使用 facade 统一处理，不再直接调用底层 client。
     * 1) 校验并加载入驻会话；
     * 2) 通过 facade 获取预授权 URL（内部处理 pre_auth_code 获取和 URL 拼接）；
     * 3) 在 redirectUri 上追加 sessionToken 作为回调参数。
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

        Long tenantId = session.getTenantId();
        Long storeId = session.getStoreId();

        // 构造自定义参数（包含 sessionToken）
        String customParam = "sessionToken=" + urlEncode(command.sessionToken());

        // 通过 facade 生成预授权 URL
        WeChatPreAuthUrlCommand preAuthCommand = WeChatPreAuthUrlCommand.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .redirectUri(command.redirectUri())
                .customParam(customParam)
                .build();

        WeChatPreAuthUrlResult result = weChatOpenPlatformFacade.buildPreAuthUrl(preAuthCommand);

        if (result == null || !StringUtils.hasText(result.getPreAuthUrl())) {
            log.error("[WechatOpenAuth] buildPreAuthUrl failed, tenantId={}, sessionToken={}",
                    tenantId, command.sessionToken());
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "生成微信授权链接失败");
        }

        String authorizeUrl = result.getPreAuthUrl();
        log.info("[WechatOpenAuth] built authorize url, tenantId={}, sessionToken={}, url={}",
                tenantId, command.sessionToken(), authorizeUrl);
        return authorizeUrl;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
