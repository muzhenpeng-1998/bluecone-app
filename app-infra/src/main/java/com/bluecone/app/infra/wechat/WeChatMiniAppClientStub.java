package com.bluecone.app.infra.wechat;

import org.springframework.stereotype.Component;

/**
 * 微信小程序网关的占位实现，后续接入真实微信 API。
 * 
 * 注意：此类不再使用 @Component 注解，而是由 WeChatClientConfiguration 手动创建 bean。
 */
public class WeChatMiniAppClientStub implements WeChatMiniAppClient {

    @Override
    public WeChatCode2SessionResult code2Session(String appId, String code) {
        throw new UnsupportedOperationException("TODO: integrate with WeChat code2session");
    }

    @Override
    public WeChatPhoneNumberResult decryptPhoneNumber(String appId, String sessionKey, String encryptedData, String iv) {
        throw new UnsupportedOperationException("TODO: integrate with WeChat phone decrypt");
    }
}
