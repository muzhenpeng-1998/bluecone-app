package com.bluecone.app.wechat.config;

import com.bluecone.app.infra.wechat.WeChatFastRegisterClient;
import com.bluecone.app.infra.wechat.WeChatFastRegisterClientStub;
import com.bluecone.app.infra.wechat.WeChatMiniAppClient;
import com.bluecone.app.infra.wechat.WeChatMiniAppClientStub;
import com.bluecone.app.infra.wechat.openplatform.WeChatOpenPlatformClient;
import com.bluecone.app.infra.wechat.openplatform.WeChatOpenPlatformClientStub;
import com.bluecone.app.infra.wechat.openplatform.WechatAuthorizedAppService;
import com.bluecone.app.infra.wechat.openplatform.WechatComponentCredentialDO;
import com.bluecone.app.infra.wechat.openplatform.WechatComponentCredentialService;
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
import org.springframework.util.StringUtils;

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
     * <p>
     * 注意：不在初始化时加载 component_verify_ticket，避免循环依赖。
     * component_verify_ticket 会在微信推送时自动保存，或在首次使用时动态加载。
     * </p>
     */
    @Bean
    @ConditionalOnProperty(prefix = "wechat.open-platform", name = "enabled", havingValue = "true")
    public WxOpenService wxOpenService(WeChatOpenPlatformProperties properties) {
        log.info("[WeChatClientConfig] 初始化 WxOpenService, componentAppId={}", properties.getComponentAppId());
        
        WxOpenInMemoryConfigStorage config = new WxOpenInMemoryConfigStorage();
        config.setComponentAppId(properties.getComponentAppId());
        config.setComponentAppSecret(properties.getComponentAppSecret());
        config.setComponentToken(properties.getComponentToken());
        config.setComponentAesKey(properties.getComponentAesKey());
        
        // 不在初始化时加载 component_verify_ticket，避免循环依赖
        // component_verify_ticket 会在以下时机设置：
        // 1. 微信每10分钟推送时，通过回调接口保存到数据库
        // 2. WechatComponentCredentialService 首次使用时会动态加载
        log.info("[WeChatClientConfig] WxOpenService 初始化完成（component_verify_ticket 将在微信推送后加载）");
        
        WxOpenServiceImpl wxOpenService = new WxOpenServiceImpl();
        wxOpenService.setWxOpenConfigStorage(config);
        
        return wxOpenService;
    }

    /**
     * 微信开放平台客户端（WxJava 实现，当 enabled=true 时创建）。
     */
    @Bean
    @ConditionalOnProperty(prefix = "wechat.open-platform", name = "enabled", havingValue = "true")
    public WeChatOpenPlatformClient wxJavaWeChatOpenPlatformClient(
            WxOpenService wxOpenService,
            WechatComponentCredentialService wechatComponentCredentialService,
            WechatAuthorizedAppService wechatAuthorizedAppService,
            WeChatOpenPlatformProperties properties) {
        log.info("[WeChatClientConfig] Using WxJavaWeChatOpenPlatformClient (WxJava 4.7.0)");
        return new WxJavaWeChatOpenPlatformClient(
                wxOpenService,
                wechatComponentCredentialService,
                wechatAuthorizedAppService,
                properties);
    }

    /**
     * 微信开放平台客户端（Stub 实现，当 enabled=false 时创建）。
     */
    @Bean
    @ConditionalOnProperty(prefix = "wechat.open-platform", name = "enabled", havingValue = "false", matchIfMissing = true)
    public WeChatOpenPlatformClient weChatOpenPlatformClientStub() {
        log.warn("[WeChatClientConfig] Using WeChatOpenPlatformClientStub (wechat.open-platform.enabled=false)");
        return new WeChatOpenPlatformClientStub();
    }

    /**
     * 微信小程序客户端（WxJava 实现，当 enabled=true 时创建）。
     */
    @Bean
    @ConditionalOnProperty(prefix = "wechat.open-platform", name = "enabled", havingValue = "true")
    public WeChatMiniAppClient wxJavaWeChatMiniAppClient(
            WxOpenService wxOpenService,
            WechatAuthorizedAppService wechatAuthorizedAppService,
            WechatComponentCredentialService wechatComponentCredentialService,
            WeChatOpenPlatformProperties properties) {
        log.info("[WeChatClientConfig] Using WxJavaWeChatMiniAppClient (WxJava 4.7.0)");
        return new WxJavaWeChatMiniAppClient(
                wxOpenService,
                wechatAuthorizedAppService,
                wechatComponentCredentialService,
                properties);
    }

    /**
     * 微信小程序客户端（Stub 实现，当 enabled=false 时创建）。
     */
    @Bean
    @ConditionalOnProperty(prefix = "wechat.open-platform", name = "enabled", havingValue = "false", matchIfMissing = true)
    public WeChatMiniAppClient weChatMiniAppClientStub() {
        log.warn("[WeChatClientConfig] Using WeChatMiniAppClientStub (wechat.open-platform.enabled=false)");
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

