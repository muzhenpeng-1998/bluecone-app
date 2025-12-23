package com.bluecone.app.wechat.config;

import com.bluecone.app.infra.wechat.WeChatFastRegisterClient;
import com.bluecone.app.infra.wechat.WeChatFastRegisterClientStub;
import com.bluecone.app.infra.wechat.WeChatMiniAppClient;
import com.bluecone.app.infra.wechat.WeChatMiniAppClientStub;
import com.bluecone.app.infra.wechat.openplatform.WeChatOpenPlatformClient;
import com.bluecone.app.infra.wechat.openplatform.WeChatOpenPlatformClientStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信客户端配置类。
 * <p>
 * 根据配置决定使用真实的 WxJava 实现还是 Stub 实现：
 * - 当 wechat.open-platform.enabled=true 时，使用 WxJavaOpenPlatformClient
 * - 否则使用 WeChatOpenPlatformClientStub（用于开发和测试）
 * </p>
 */
@Configuration
public class WeChatClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(WeChatClientConfiguration.class);

    /**
     * 微信开放平台客户端（当前使用 Stub 实现）。
     * <p>
     * 注意：WxJava SDK 实现因版本兼容性问题暂时禁用。
     * 生产环境建议使用 HTTP 客户端直接调用微信 API。
     * 详见：docs/wechat-wxjava-integration-notes.md
     * </p>
     */
    @Bean
    public WeChatOpenPlatformClient weChatOpenPlatformClient() {
        log.warn("[WeChatClientConfig] Using WeChatOpenPlatformClientStub (WxJava implementation disabled due to version compatibility issues)");
        return new WeChatOpenPlatformClientStub();
    }

    /**
     * 微信小程序客户端（当前使用 Stub 实现）。
     * <p>
     * 注意：WxJava SDK 实现因版本兼容性问题暂时禁用。
     * 生产环境建议使用 HTTP 客户端直接调用微信 API。
     * 详见：docs/wechat-wxjava-integration-notes.md
     * </p>
     */
    @Bean
    public WeChatMiniAppClient weChatMiniAppClient() {
        log.warn("[WeChatClientConfig] Using WeChatMiniAppClientStub (WxJava implementation disabled due to version compatibility issues)");
        return new WeChatMiniAppClientStub();
    }

    /**
     * 微信快速注册客户端（当前使用 Stub 实现）。
     * <p>
     * 用于小程序快速注册功能。
     * 注意：WxJava SDK 实现因版本兼容性问题暂时禁用。
     * 生产环境建议使用 HTTP 客户端直接调用微信 API。
     * </p>
     */
    @Bean
    public WeChatFastRegisterClient weChatFastRegisterClient() {
        log.warn("[WeChatClientConfig] Using WeChatFastRegisterClientStub (WxJava implementation disabled due to version compatibility issues)");
        return new WeChatFastRegisterClientStub();
    }
}

