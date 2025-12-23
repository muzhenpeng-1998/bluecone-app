package com.bluecone.app.infra.wechat.openplatform;

import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 微信开放平台客户端 Stub 实现。
 *
 * 用于本地开发初期 / 单元测试时的占位实现，不真正调用微信接口。
 * 后续会在同包下新增基于 WebClient 或 RestTemplate 的正式实现。
 * 
 * 注意：此类不再使用 @Component 注解，而是由 WeChatClientConfiguration 手动创建 bean。
 */
public class WeChatOpenPlatformClientStub implements WeChatOpenPlatformClient {

    private static final Logger log = LoggerFactory.getLogger(WeChatOpenPlatformClientStub.class);

    @Override
    public ComponentAccessTokenResult getComponentAccessToken(String componentAppId,
                                                              String componentAppSecret,
                                                              String componentVerifyTicket) {
        log.warn("[WeChatOpenPlatform][Stub] getComponentAccessToken called, return fake token");
        ComponentAccessTokenResult result = new ComponentAccessTokenResult();
        result.setComponentAccessToken("stub-component-access-token");
        result.setExpiresIn(7200);
        result.setObtainedAt(Instant.now());
        result.setErrcode(0);
        result.setErrmsg("ok");
        return result;
    }

    @Override
    public PreAuthCodeResult createPreAuthCode(String componentAccessToken) {
        log.warn("[WeChatOpenPlatform][Stub] createPreAuthCode called, return fake pre_auth_code");
        PreAuthCodeResult result = new PreAuthCodeResult();
        result.setPreAuthCode("stub-pre-auth-code");
        result.setExpiresIn(600);
        result.setObtainedAt(Instant.now());
        result.setErrcode(0);
        result.setErrmsg("ok");
        return result;
    }

    @Override
    public QueryAuthResult queryAuth(String componentAccessToken, String authorizationCode) {
        log.warn("[WeChatOpenPlatform][Stub] queryAuth called, return fake auth result, authorizationCode={}",
                authorizationCode);
        QueryAuthResult result = new QueryAuthResult();
        result.setErrcode(0);
        result.setErrmsg("ok");
        // 这里给一个假 appid，方便后续联调流程
        result.setAuthorizerAppId("wx_stub_authorizer_appid");
        result.setAuthorizerRefreshToken("stub-refresh-token");
        return result;
    }

    @Override
    public Optional<AuthorizerInfoResult> getAuthorizerInfo(String componentAccessToken,
                                                            String authorizerAppId) {
        log.warn("[WeChatOpenPlatform][Stub] getAuthorizerInfo called, authorizerAppId={}", authorizerAppId);
        AuthorizerInfoResult result = new AuthorizerInfoResult();
        result.setErrcode(0);
        result.setErrmsg("ok");
        result.setAuthorizerAppId(authorizerAppId);
        result.setNickName("Stub 小程序");
        result.setPrincipalName("Stub 主体");
        result.setVerifyType(0);
        return Optional.of(result);
    }

    @Override
    public RefreshAuthorizerTokenResult refreshAuthorizerToken(String componentAccessToken,
                                                               String componentAppId,
                                                               String authorizerAppId,
                                                               String authorizerRefreshToken) {
        log.warn("[WeChatOpenPlatform][Stub] refreshAuthorizerToken called, return fake token");
        RefreshAuthorizerTokenResult result = new RefreshAuthorizerTokenResult();
        result.setAuthorizerAccessToken("stub-authorizer-access-token-refreshed");
        result.setAuthorizerRefreshToken(authorizerRefreshToken);
        result.setExpiresInSeconds(7200);
        return result;
    }
}

