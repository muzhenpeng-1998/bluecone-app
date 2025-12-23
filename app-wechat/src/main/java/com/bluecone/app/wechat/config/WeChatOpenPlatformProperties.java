package com.bluecone.app.wechat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信开放平台配置属性。
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.open-platform")
public class WeChatOpenPlatformProperties {

    /**
     * 是否启用微信开放平台功能（本地开发可设为 false 使用 Stub）
     */
    private boolean enabled = true;

    /**
     * 第三方平台 appid
     */
    private String componentAppId;

    /**
     * 第三方平台 appsecret
     */
    private String componentAppSecret;

    /**
     * 消息校验 Token
     */
    private String componentToken;

    /**
     * 消息加解密 Key
     */
    private String componentAesKey;
}

