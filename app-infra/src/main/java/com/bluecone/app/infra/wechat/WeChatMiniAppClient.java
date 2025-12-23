package com.bluecone.app.infra.wechat;

/**
 * 微信小程序网关接口，封装 code2session 与手机号解密。
 */
public interface WeChatMiniAppClient {

    /**
     * 使用 wx.login 拿到的 code 换取 openId/unionId/sessionKey。
     */
    WeChatCode2SessionResult code2Session(String appId, String code);

    /**
     * 解密手机号（旧版本，使用 sessionKey + encryptedData + iv）。
     */
    WeChatPhoneNumberResult decryptPhoneNumber(String appId, String sessionKey, String encryptedData, String iv);

    /**
     * 获取手机号（新版本，使用 phoneCode）。
     * 
     * 对应接口：
     * POST https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=ACCESS_TOKEN
     * 
     * 注意：此接口需要 authorizer_access_token（第三方平台模式）。
     *
     * @param authorizerAppId 授权方小程序 appId
     * @param phoneCode       手机号 code（通过 wx.getPhoneNumber 获取）
     * @return 手机号信息
     */
    WeChatPhoneNumberResult getPhoneNumberByCode(String authorizerAppId, String phoneCode);
}
