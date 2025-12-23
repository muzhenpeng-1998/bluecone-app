package com.bluecone.app.infra.wechat.openplatform;

import java.util.Optional;

/**
 * 微信开放平台（第三方平台）HTTP 客户端网关。
 *
 * 只暴露与业务相关的高层操作，不直接泄漏底层 HTTP 细节。
 * 后续所有“代注册小程序 / 授权 / 获取信息”等都从这里进入。
 */
public interface WeChatOpenPlatformClient {

    /**
     * 获取 component_access_token。
     *
     * 对应微信开放平台接口：
     * POST https://api.weixin.qq.com/cgi-bin/component/api_component_token
     */
    ComponentAccessTokenResult getComponentAccessToken(String componentAppId,
                                                       String componentAppSecret,
                                                       String componentVerifyTicket);

    /**
     * 为指定第三方平台创建预授权码 pre_auth_code。
     *
     * 对应接口：
     * POST https://api.weixin.qq.com/cgi-bin/component/api_create_preauthcode?component_access_token=XXX
     */
    PreAuthCodeResult createPreAuthCode(String componentAccessToken);

    /**
     * 使用授权码查询授权信息。
     *
     * 对应接口：
     * POST https://api.weixin.qq.com/cgi-bin/component/api_query_auth?component_access_token=XXX
     */
    QueryAuthResult queryAuth(String componentAccessToken, String authorizationCode);

    /**
     * 获取已授权方（小程序）的基本信息。
     *
     * 对应接口：
     * POST https://api.weixin.qq.com/cgi-bin/component/api_get_authorizer_info?component_access_token=XXX
     */
    Optional<AuthorizerInfoResult> getAuthorizerInfo(String componentAccessToken,
                                                     String authorizerAppId);

    /**
     * 刷新授权方的接口调用令牌（authorizer_access_token）。
     *
     * 对应接口：
     * POST https://api.weixin.qq.com/cgi-bin/component/api_authorizer_token?component_access_token=XXX
     *
     * @param componentAccessToken  第三方平台 component_access_token
     * @param componentAppId        第三方平台 appid
     * @param authorizerAppId       授权方 appid
     * @param authorizerRefreshToken 授权方的刷新令牌
     * @return 刷新结果
     */
    RefreshAuthorizerTokenResult refreshAuthorizerToken(String componentAccessToken,
                                                        String componentAppId,
                                                        String authorizerAppId,
                                                        String authorizerRefreshToken);
}

