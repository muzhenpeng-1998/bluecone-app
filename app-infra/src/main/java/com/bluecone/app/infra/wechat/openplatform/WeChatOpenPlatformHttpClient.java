package com.bluecone.app.infra.wechat.openplatform;

import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 微信开放平台 HTTP 客户端正式实现（骨架）。
 *
 * TODO: 后续使用 WebClient 或 RestTemplate 实现真实 HTTP 调用，
 * 并通过配置切换替代 Stub 实现。
 */
/**
 * 微信开放平台 HTTP 客户端正式实现（骨架）。
 *
 * TODO: 后续使用 WebClient 或 RestTemplate 实现真实 HTTP 调用，
 * 并通过配置切换替代 Stub 实现。
 * 
 * 说明：此实现仅在非 local/dev/stub-wechat profile 下激活，避免与 Stub 实现冲突。
 */
@Component
@Profile("!local & !dev & !stub-wechat")
public class WeChatOpenPlatformHttpClient implements WeChatOpenPlatformClient {

    @Override
    public ComponentAccessTokenResult getComponentAccessToken(String componentAppId,
                                                              String componentAppSecret,
                                                              String componentVerifyTicket) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PreAuthCodeResult createPreAuthCode(String componentAccessToken) {
        // TODO: 使用 RestTemplate 调用
        // String url = "https://api.weixin.qq.com/cgi-bin/component/api_create_preauthcode?component_access_token=" + componentAccessToken;
        // 构造请求体 {"component_appid": "..."} 并解析响应为 PreAuthCodeResult。
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public QueryAuthResult queryAuth(String componentAccessToken, String authorizationCode) {
        // TODO: 使用 RestTemplate 调用
        // String url = "https://api.weixin.qq.com/cgi-bin/component/api_query_auth?component_access_token=" + componentAccessToken;
        // 请求体包含 component_appid 与 authorization_code，解析响应为 QueryAuthResult。
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<AuthorizerInfoResult> getAuthorizerInfo(String componentAccessToken,
                                                            String authorizerAppId) {
        // TODO: 使用 RestTemplate 调用
        // String url = "https://api.weixin.qq.com/cgi-bin/component/api_get_authorizer_info?component_access_token=" + componentAccessToken;
        // 请求体包含 component_appid 与 authorizer_appid，解析响应为 AuthorizerInfoResult。
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
