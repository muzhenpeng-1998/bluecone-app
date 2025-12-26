package com.bluecone.app.wechat.facade.miniapp;

/**
 * 微信小程序 Facade 接口。
 * <p>
 * 对外暴露小程序登录、手机号获取等功能，隐藏 WxJava SDK 实现细节。
 * 所有入参只允许 tenantId/storeId + code/phoneCode，不允许 authorizerAppId 等内部字段。
 * </p>
 */
public interface WeChatMiniAppFacade {

    /**
     * 小程序登录：使用 wx.login 获取的 code 换取 openId/unionId/sessionKey。
     * <p>
     * 内部流程：
     * 1. 根据 tenantId/storeId 路由到对应的 authorizerAppId
     * 2. 使用开放平台 component 模式调用 jscode2session
     * 3. 返回 openId/unionId/sessionKey（sessionKey 可选，用于后续解密）
     * </p>
     *
     * @param command 登录命令（包含 tenantId/storeId/code）
     * @return 登录结果（包含 openId/unionId/sessionKey）
     */
    WeChatMiniAppLoginResult code2Session(WeChatMiniAppCode2SessionCommand command);

    /**
     * 获取手机号：使用 wx.getPhoneNumber 获取的 phoneCode 换取手机号。
     * <p>
     * 内部流程：
     * 1. 根据 tenantId/storeId 路由到对应的 authorizerAppId
     * 2. 获取或刷新 authorizer_access_token
     * 3. 调用微信小程序手机号接口
     * 4. 返回手机号和国家码
     * </p>
     *
     * @param command 手机号获取命令（包含 tenantId/storeId/phoneCode）
     * @return 手机号结果（包含 phoneNumber/countryCode）
     */
    WeChatMiniAppPhoneResult getPhoneNumber(WeChatMiniAppPhoneCommand command);
}

