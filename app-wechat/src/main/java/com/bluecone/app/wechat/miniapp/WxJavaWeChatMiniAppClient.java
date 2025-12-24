package com.bluecone.app.wechat.miniapp;

import cn.binarywang.wx.miniapp.util.crypt.WxMaCryptUtils;
import com.bluecone.app.infra.wechat.WeChatCode2SessionResult;
import com.bluecone.app.infra.wechat.WeChatMiniAppClient;
import com.bluecone.app.infra.wechat.WeChatPhoneNumberResult;
import com.bluecone.app.infra.wechat.openplatform.WechatAuthorizedAppService;
import com.bluecone.app.infra.wechat.openplatform.WechatComponentCredentialService;
import com.bluecone.app.wechat.config.WeChatOpenPlatformProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.open.api.WxOpenComponentService;
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
 * 
 * <p>
 * 最小 curl 示例（小程序登录）：
 * <pre>
 * POST /api/open/user/auth/wechat/miniapp/login
 * Content-Type: application/json
 * 
 * {
 *   "authorizerAppId": "wx1234567890abcdef",
 *   "code": "081XYZ...",
 *   "phoneCode": "abc123..."
 * }
 * </pre>
 * </p>
 */
public class WxJavaWeChatMiniAppClient implements WeChatMiniAppClient {

    private static final Logger log = LoggerFactory.getLogger(WxJavaWeChatMiniAppClient.class);

    private final WxOpenService wxOpenService;
    private final WechatAuthorizedAppService wechatAuthorizedAppService;
    private final WechatComponentCredentialService wechatComponentCredentialService;
    private final WeChatOpenPlatformProperties properties;
    private final ObjectMapper objectMapper;

    public WxJavaWeChatMiniAppClient(
            WxOpenService wxOpenService,
            WechatAuthorizedAppService wechatAuthorizedAppService,
            WechatComponentCredentialService wechatComponentCredentialService,
            WeChatOpenPlatformProperties properties) {
        this.wxOpenService = wxOpenService;
        this.wechatAuthorizedAppService = wechatAuthorizedAppService;
        this.wechatComponentCredentialService = wechatComponentCredentialService;
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        log.info("[WxJavaWeChatMiniAppClient] 初始化完成");
    }

    /**
     * 小程序登录：code2Session（第三方平台模式）。
     * <p>
     * 调用微信开放平台接口：GET https://api.weixin.qq.com/sns/component/jscode2session
     * </p>
     * <p>
     * 注意：第三方平台模式下，需要使用 component_access_token 而不是普通的 access_token。
     * </p>
     *
     * @param appId 小程序 appid（授权方 appid）
     * @param code  小程序登录凭证 code
     * @return code2session 结果（包含 openid、session_key、unionid）
     */
    @Override
    public WeChatCode2SessionResult code2Session(String appId, String code) {
        log.info("[WxJavaWeChatMiniAppClient] code2Session, appId={}, code={}", 
                maskAppId(appId), maskCode(code));
        
        try {
            // 1. 获取有效的 component_access_token
            String componentAccessToken = wechatComponentCredentialService.getValidComponentAccessToken();
            log.debug("[WxJavaWeChatMiniAppClient] 获取到 component_access_token={}...", 
                    maskToken(componentAccessToken));
            
            // 2. 获取 component_appid
            String componentAppId = wxOpenService.getWxOpenConfigStorage().getComponentAppId();
            
            // 3. 拼接 URL（第三方平台模式的 jscode2session）
            String url = String.format(
                    WxOpenComponentService.MINIAPP_JSCODE_2_SESSION,
                    appId, code, componentAppId, componentAccessToken);
            
            log.debug("[WxJavaWeChatMiniAppClient] code2Session URL: {}", 
                    url.replaceAll("component_access_token=[^&]+", "component_access_token=***"));
            
            // 4. 调用微信接口（使用 GET 请求）
            WxOpenComponentService componentService = wxOpenService.getWxOpenComponentService();
            String responseJson = componentService.get(url, null);
            
            log.debug("[WxJavaWeChatMiniAppClient] code2Session response: {}", responseJson);
            
            // 5. 解析响应
            JsonNode jsonNode = objectMapper.readTree(responseJson);
            
            // 检查是否有错误
            if (jsonNode.has("errcode") && jsonNode.get("errcode").asInt() != 0) {
                int errcode = jsonNode.get("errcode").asInt();
                String errmsg = jsonNode.has("errmsg") ? jsonNode.get("errmsg").asText() : "unknown error";
                log.error("[WxJavaWeChatMiniAppClient] code2Session 失败, appId={}, errcode={}, errmsg={}", 
                        maskAppId(appId), errcode, errmsg);
                throw new IllegalStateException(
                        String.format("code2Session 失败, errcode=%d, errmsg=%s", errcode, errmsg));
            }
            
            // 6. 映射到结果对象
            WeChatCode2SessionResult result = new WeChatCode2SessionResult();
            
            if (jsonNode.has("openid")) {
                result.setOpenId(jsonNode.get("openid").asText());
            }
            if (jsonNode.has("session_key")) {
                result.setSessionKey(jsonNode.get("session_key").asText());
            }
            if (jsonNode.has("unionid")) {
                result.setUnionId(jsonNode.get("unionid").asText());
            }
            
            log.info("[WxJavaWeChatMiniAppClient] code2Session 成功, appId={}, openId={}", 
                    maskAppId(appId), maskOpenId(result.getOpenId()));
            
            return result;
            
        } catch (WxErrorException e) {
            log.error("[WxJavaWeChatMiniAppClient] code2Session 失败, appId={}, errcode={}, errmsg={}", 
                    maskAppId(appId), e.getError().getErrorCode(), e.getError().getErrorMsg());
            throw new IllegalStateException(
                    String.format("code2Session 失败, errcode=%d, errmsg=%s",
                            e.getError().getErrorCode(), e.getError().getErrorMsg()), e);
        } catch (Exception e) {
            log.error("[WxJavaWeChatMiniAppClient] code2Session 异常, appId={}", maskAppId(appId), e);
            throw new IllegalStateException("code2Session 异常: " + e.getMessage(), e);
        }
    }

    /**
     * 通过 phoneCode 获取手机号（新版接口，推荐）。
     * <p>
     * 调用微信小程序接口：POST https://api.weixin.qq.com/wxa/business/getuserphonenumber
     * </p>
     * <p>
     * 注意：需要使用授权方的 authorizer_access_token。
     * </p>
     *
     * @param authorizerAppId 授权方小程序 appid
     * @param phoneCode       手机号获取凭证（从小程序前端获取）
     * @return 手机号信息
     */
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
            
            // 2. 构造请求 URL（微信小程序获取手机号接口）
            String url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + authorizerAccessToken;
            
            // 3. 构造请求 body
            String requestBody = String.format("{\"code\":\"%s\"}", phoneCode);
            
            // 4. 调用微信接口（使用 WxOpenComponentService 的 post 方法）
            WxOpenComponentService componentService = wxOpenService.getWxOpenComponentService();
            String responseJson = componentService.post(url, requestBody);
            
            log.debug("[WxJavaWeChatMiniAppClient] getPhoneNumberByCode response: {}", responseJson);
            
            // 5. 解析响应
            JsonNode jsonNode = objectMapper.readTree(responseJson);
            
            // 检查是否有错误
            if (jsonNode.has("errcode") && jsonNode.get("errcode").asInt() != 0) {
                int errcode = jsonNode.get("errcode").asInt();
                String errmsg = jsonNode.has("errmsg") ? jsonNode.get("errmsg").asText() : "unknown error";
                log.error("[WxJavaWeChatMiniAppClient] getPhoneNumberByCode 失败, authorizerAppId={}, errcode={}, errmsg={}", 
                        maskAppId(authorizerAppId), errcode, errmsg);
                throw new IllegalStateException(
                        String.format("获取手机号失败, errcode=%d, errmsg=%s", errcode, errmsg));
            }
            
            // 6. 解析 phone_info
            WeChatPhoneNumberResult result = new WeChatPhoneNumberResult();
            
            if (jsonNode.has("phone_info")) {
                JsonNode phoneInfo = jsonNode.get("phone_info");
                if (phoneInfo.has("phoneNumber")) {
                    result.setPhoneNumber(phoneInfo.get("phoneNumber").asText());
                }
                if (phoneInfo.has("countryCode")) {
                    result.setCountryCode(phoneInfo.get("countryCode").asText());
                }
            }
            
            log.info("[WxJavaWeChatMiniAppClient] getPhoneNumberByCode 成功, authorizerAppId={}, phone={}", 
                    maskAppId(authorizerAppId), maskPhone(result.getPhoneNumber()));
            
            return result;
            
        } catch (WxErrorException e) {
            log.error("[WxJavaWeChatMiniAppClient] getPhoneNumberByCode 失败, authorizerAppId={}, errcode={}, errmsg={}", 
                    maskAppId(authorizerAppId), e.getError().getErrorCode(), e.getError().getErrorMsg());
            throw new IllegalStateException(
                    String.format("获取手机号失败, errcode=%d, errmsg=%s",
                            e.getError().getErrorCode(), e.getError().getErrorMsg()), e);
        } catch (Exception e) {
            log.error("[WxJavaWeChatMiniAppClient] getPhoneNumberByCode 异常, authorizerAppId={}", 
                    maskAppId(authorizerAppId), e);
            throw new IllegalStateException("获取手机号异常: " + e.getMessage(), e);
        }
    }

    /**
     * 解密手机号（旧版接口，兼容 encryptedData + iv 方式）。
     * <p>
     * 使用 WxJava 的解密工具 WxMaCryptUtils 进行 AES 解密。
     * </p>
     * <p>
     * 注意：新版推荐使用 getPhoneNumberByCode 接口，此方法仅用于兼容旧版本。
     * </p>
     *
     * @param appId         小程序 appid
     * @param sessionKey    会话密钥（从 code2Session 获取）
     * @param encryptedData 加密数据
     * @param iv            加密算法的初始向量
     * @return 手机号信息
     */
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
        
        if (!StringUtils.hasText(sessionKey)) {
            log.error("[WxJavaWeChatMiniAppClient] sessionKey 为空，无法解密");
            throw new IllegalArgumentException("sessionKey 不能为空");
        }
        
        try {
            // 使用 WxJava 的解密工具
            String decryptedJson = WxMaCryptUtils.decrypt(sessionKey, encryptedData, iv);
            
            log.debug("[WxJavaWeChatMiniAppClient] decryptPhoneNumber 解密结果: {}", decryptedJson);
            
            // 解析 JSON
            JsonNode jsonNode = objectMapper.readTree(decryptedJson);
            
            WeChatPhoneNumberResult result = new WeChatPhoneNumberResult();
            
            if (jsonNode.has("phoneNumber")) {
                result.setPhoneNumber(jsonNode.get("phoneNumber").asText());
            }
            if (jsonNode.has("countryCode")) {
                result.setCountryCode(jsonNode.get("countryCode").asText());
            }
            
            log.info("[WxJavaWeChatMiniAppClient] decryptPhoneNumber 成功, appId={}, phone={}", 
                    maskAppId(appId), maskPhone(result.getPhoneNumber()));
            
            return result;
            
        } catch (Exception e) {
            log.error("[WxJavaWeChatMiniAppClient] decryptPhoneNumber 解密失败, appId={}", 
                    maskAppId(appId), e);
            throw new IllegalStateException("解密手机号失败: " + e.getMessage(), e);
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
