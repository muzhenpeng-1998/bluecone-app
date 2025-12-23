package com.bluecone.app.wechat.miniapp;

import com.bluecone.app.infra.wechat.WeChatCode2SessionResult;
import com.bluecone.app.infra.wechat.WeChatMiniAppClient;
import com.bluecone.app.infra.wechat.WeChatPhoneNumberResult;
import com.bluecone.app.infra.wechat.openplatform.WechatAuthorizedAppService;
import com.bluecone.app.infra.wechat.openplatform.WechatComponentCredentialService;
import com.bluecone.app.wechat.config.WeChatOpenPlatformProperties;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.open.api.WxOpenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * 基于 WxJava SDK 的微信小程序客户端实现（第三方平台模式）。
 * <p>
 * 使用 WxJava 4.7.0 的开放平台 API 实现微信小程序功能。
 * 注意：第三方平台模式下，需要使用 WxOpenService 来处理小程序相关操作。
 * </p>
 */
public class WxJavaWeChatMiniAppClient implements WeChatMiniAppClient {

    private static final Logger log = LoggerFactory.getLogger(WxJavaWeChatMiniAppClient.class);

    private final WxOpenService wxOpenService;
    private final WechatAuthorizedAppService wechatAuthorizedAppService;
    private final WechatComponentCredentialService wechatComponentCredentialService;
    private final WeChatOpenPlatformProperties properties;

    public WxJavaWeChatMiniAppClient(
            WxOpenService wxOpenService,
            WechatAuthorizedAppService wechatAuthorizedAppService,
            WechatComponentCredentialService wechatComponentCredentialService,
            WeChatOpenPlatformProperties properties) {
        this.wxOpenService = wxOpenService;
        this.wechatAuthorizedAppService = wechatAuthorizedAppService;
        this.wechatComponentCredentialService = wechatComponentCredentialService;
        this.properties = properties;
        log.info("[WxJavaWeChatMiniAppClient] 初始化完成");
    }

    @Override
    public WeChatCode2SessionResult code2Session(String appId, String code) {
        log.info("[WxJavaWeChatMiniAppClient] code2Session, appId={}, code={}", 
                maskAppId(appId), maskCode(code));
        
        try {
            // 第三方平台模式下，通过 WxOpenService 的小程序服务进行 code2Session
            // TODO: 根据实际 WxJava 4.7.0 API 调整方法调用
            // 可能的方法: wxOpenService.getWxOpenComponentService().miniappJscode2Session(appId, code)
            // 或: wxOpenService.getWxMaServiceByAppid(appId).getUserService().getSessionInfo(code)
            
            throw new UnsupportedOperationException(
                    "code2Session 需要根据 WxJava 4.7.0 实际 API 实现。" +
                    "请参考 WxJava 文档：https://github.com/Wechat-Group/WxJava");
            
        } catch (Exception e) {
            log.error("[WxJavaWeChatMiniAppClient] code2Session 异常", e);
            throw new IllegalStateException("code2Session 异常: " + e.getMessage(), e);
        }
    }

    @Override
    public WeChatPhoneNumberResult decryptPhoneNumber(
            String appId,
            String sessionKey,
            String encryptedData,
            String iv) {
        log.info("[WxJavaWeChatMiniAppClient] decryptPhoneNumber (旧版), appId={}", 
                maskAppId(appId));
        
        // 兼容旧版本：如果没有 encryptedData 或 iv，直接返回 null
        if (!StringUtils.hasText(encryptedData) || !StringUtils.hasText(iv)) {
            log.warn("[WxJavaWeChatMiniAppClient] encryptedData 或 iv 为空，返回 null");
            return null;
        }
        
        // TODO: 实现解密逻辑（需要使用 WxJava 的解密工具或自行实现 AES 解密）
        log.warn("[WxJavaWeChatMiniAppClient] decryptPhoneNumber 暂未实现，返回 null");
        return null;
    }

    @Override
    public WeChatPhoneNumberResult getPhoneNumberByCode(String authorizerAppId, String phoneCode) {
        log.info("[WxJavaWeChatMiniAppClient] getPhoneNumberByCode, authorizerAppId={}, phoneCode={}", 
                maskAppId(authorizerAppId), maskCode(phoneCode));
        
        try {
            // 1. 获取或刷新 authorizer_access_token
            String authorizerAccessToken = wechatAuthorizedAppService
                    .getOrRefreshAuthorizerAccessToken(authorizerAppId);
            
            log.debug("[WxJavaWeChatMiniAppClient] 获取到 authorizer_access_token={}...", 
                    maskToken(authorizerAccessToken));
            
            // 2. 调用获取手机号接口（使用 WxOpenService 的小程序服务）
            // TODO: 根据实际 WxJava 4.7.0 API 调整方法调用
            // 可能的方法: wxOpenService.getWxMaServiceByAppid(authorizerAppId).getUserService().getPhoneNoInfo(phoneCode)
            
            throw new UnsupportedOperationException(
                    "getPhoneNumberByCode 需要根据 WxJava 4.7.0 实际 API 实现。" +
                    "请参考 WxJava 文档：https://github.com/Wechat-Group/WxJava");
            
        } catch (Exception e) {
            log.error("[WxJavaWeChatMiniAppClient] getPhoneNumberByCode 异常", e);
            throw new IllegalStateException("获取手机号异常: " + e.getMessage(), e);
        }
    }

    /**
     * 脱敏 AppID（只显示前 6 位和后 4 位）
     */
    private String maskAppId(String appId) {
        if (appId == null || appId.length() <= 10) {
            return "***";
        }
        return appId.substring(0, 6) + "****" + appId.substring(appId.length() - 4);
    }

    /**
     * 脱敏 OpenID（只显示前 8 位）
     */
    private String maskOpenId(String openId) {
        if (openId == null || openId.length() <= 8) {
            return "***";
        }
        return openId.substring(0, 8) + "...";
    }

    /**
     * 脱敏 Code（只显示前 8 位）
     */
    private String maskCode(String code) {
        if (code == null || code.length() <= 8) {
            return "***";
        }
        return code.substring(0, 8) + "...";
    }

    /**
     * 脱敏 Token（只显示前 8 位）
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 8) + "...";
    }

    /**
     * 脱敏手机号（只显示前 3 位和后 4 位）
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
