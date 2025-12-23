package com.bluecone.app.wechat.config;

import com.bluecone.app.infra.wechat.WeChatFastRegisterClient;
import com.bluecone.app.infra.wechat.WeChatFastRegisterClientStub;
import com.bluecone.app.infra.wechat.WeChatMiniAppClient;
import com.bluecone.app.infra.wechat.WeChatMiniAppClientStub;
import com.bluecone.app.infra.wechat.openplatform.WeChatOpenPlatformClient;
import com.bluecone.app.infra.wechat.openplatform.WeChatOpenPlatformClientStub;
import com.bluecone.app.wechat.miniapp.WxJavaWeChatMiniAppClient;
import com.bluecone.app.wechat.openplatform.WxJavaWeChatOpenPlatformClient;
import me.chanjar.weixin.open.api.WxOpenService;
import me.chanjar.weixin.open.api.impl.WxOpenInMemoryConfigStorage;
import me.chanjar.weixin.open.api.impl.WxOpenServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信客户端配置类。
 * <p>
 * 根据配置决定使用真实的 WxJava 实现还是 Stub 实现：
 * - 当 wechat.open-platform.enabled=true 时，使用 WxJava 实现
 * - 否则使用 Stub 实现（用于开发和测试）
 * </p>
 */
@Configuration
public class WeChatClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(WeChatClientConfiguration.class);

    /**
     * WxJava 开放平台 Service（当 enabled=true 时创建）。
     * 
     * 注意：WxJava 4.7.0 的包结构可能有变化，暂时注释掉，使用 Stub 实现。
     * TODO: 验证 WxJava 4.7.0 的正确用法后再启用。
     */
    // @Bean
    // @ConditionalOnProperty(prefix = "wechat.open-platform", name = "enabled", havingValue = "true")
    // public WxOpenService wxOpenService(WeChatOpenPlatformProperties properties) {
    //     log.info("[WeChatClientConfig] 初始化 WxOpenService, componentAppId={}", properties.getComponentAppId());
    //     
    //     WxOpenInMemoryConfigStorage config = new WxOpenInMemoryConfigStorage();
    //     config.setComponentAppId(properties.getComponentAppId());
    //     config.setComponentAppSecret(properties.getComponentAppSecret());
    //     config.setComponentToken(properties.getComponentToken());
    //     config.setComponentAesKey(properties.getComponentAesKey());
    //     
    //     WxOpenServiceImpl wxOpenService = new WxOpenServiceImpl();
    //     wxOpenService.setWxOpenConfigStorage(config);
    //     
    //     log.info("[WeChatClientConfig] WxOpenService 初始化完成");
    //     return wxOpenService;
    // }

    /**
     * 微信开放平台客户端（WxJava 实现，当 enabled=true 时创建）。
     * 
     * 注意：WxJava 4.7.0 的包结构可能有变化，暂时注释掉，使用 Stub 实现。
     * TODO: 验证 WxJava 4.7.0 的正确用法后再启用。
     */
    // @Bean
    // @ConditionalOnProperty(prefix = "wechat.open-platform", name = "enabled", havingValue = "true")
    // public WeChatOpenPlatformClient wxJavaWeChatOpenPlatformClient(WxOpenService wxOpenService) {
    //     log.info("[WeChatClientConfig] Using WxJavaWeChatOpenPlatformClient (WxJava 4.7.0)");
    //     return new WxJavaWeChatOpenPlatformClient(wxOpenService);
    // }

    /**
     * 微信开放平台客户端（Stub 实现）。
     * 
     * 注意：由于 WxJava 4.7.0 实现暂时注释掉，这里总是创建 Stub 实现。
     * TODO: 验证 WxJava 4.7.0 后，改为条件创建。
     */
    @Bean
    public WeChatOpenPlatformClient weChatOpenPlatformClient() {
        log.warn("[WeChatClientConfig] Using WeChatOpenPlatformClientStub (WxJava implementation temporarily disabled)");
        return new WeChatOpenPlatformClientStub();
    }

    /**
     * 微信小程序客户端（Stub 实现）。
     * 
     * 注意：由于 WxJava 4.7.0 实现暂时注释掉，这里总是创建 Stub 实现。
     * TODO: 验证 WxJava 4.7.0 后，改为条件创建。
     */
    @Bean
    public WeChatMiniAppClient weChatMiniAppClient() {
        log.warn("[WeChatClientConfig] Using WeChatMiniAppClientStub (WxJava implementation temporarily disabled)");
        return new WeChatMiniAppClientStub();
    }

    /**
     * 微信快速注册客户端（当前使用 Stub 实现）。
     * <p>
     * 用于小程序快速注册功能。
     * </p>
     */
    @Bean
    public WeChatFastRegisterClient weChatFastRegisterClient() {
        log.warn("[WeChatClientConfig] Using WeChatFastRegisterClientStub (fast register not implemented yet)");
        return new WeChatFastRegisterClientStub();
    }
}

