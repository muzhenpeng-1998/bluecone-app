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
     * 解密手机号。
     */
    WeChatPhoneNumberResult decryptPhoneNumber(String appId, String sessionKey, String encryptedData, String iv);
}
