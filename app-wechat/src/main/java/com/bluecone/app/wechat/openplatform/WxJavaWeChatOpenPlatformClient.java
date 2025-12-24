package com.bluecone.app.wechat.openplatform;

import com.bluecone.app.infra.wechat.openplatform.*;
import com.bluecone.app.wechat.config.WeChatOpenPlatformProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.open.api.WxOpenComponentService;
import me.chanjar.weixin.open.api.WxOpenService;
import me.chanjar.weixin.open.bean.WxOpenComponentAccessToken;
import me.chanjar.weixin.open.bean.WxOpenAuthorizerAccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 基于 WxJava SDK 的微信开放平台客户端实现。
 * <p>
 * 使用 WxJava 4.7.0 的开放平台 API 实现微信第三方平台功能。
 * </p>
 */
public class WxJavaWeChatOpenPlatformClient implements WeChatOpenPlatformClient {

    private static final Logger log = LoggerFactory.getLogger(WxJavaWeChatOpenPlatformClient.class);

    private final WxOpenService wxOpenService;
    private final WechatComponentCredentialService wechatComponentCredentialService;
    private final WechatAuthorizedAppService wechatAuthorizedAppService;
    private final WeChatOpenPlatformProperties properties;
    private final ObjectMapper objectMapper;

    public WxJavaWeChatOpenPlatformClient(
            WxOpenService wxOpenService,
            WechatComponentCredentialService wechatComponentCredentialService,
            WechatAuthorizedAppService wechatAuthorizedAppService,
            WeChatOpenPlatformProperties properties) {
        this.wxOpenService = wxOpenService;
        this.wechatComponentCredentialService = wechatComponentCredentialService;
        this.wechatAuthorizedAppService = wechatAuthorizedAppService;
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        log.info("[WxJavaWeChatOpenPlatformClient] 初始化完成, componentAppId={}", 
                properties.getComponentAppId());
    }

    /**
     * 获取 component_access_token。
     * <p>
     * 调用微信开放平台接口：POST https://api.weixin.qq.com/cgi-bin/component/api_component_token
     * </p>
     *
     * @param componentAppId       第三方平台 appid
     * @param componentAppSecret   第三方平台 appsecret
     * @param componentVerifyTicket 微信后台推送的 ticket
     * @return component_access_token 获取结果
     */
    @Override
    public ComponentAccessTokenResult getComponentAccessToken(
            String componentAppId,
            String componentAppSecret,
            String componentVerifyTicket) {
        log.info("[WxJavaWeChatOpenPlatformClient] getComponentAccessToken, componentAppId={}", 
                maskAppId(componentAppId));
        
        ComponentAccessTokenResult result = new ComponentAccessTokenResult();
        result.setObtainedAt(Instant.now());
        
        try {
            WxOpenComponentService componentService = wxOpenService.getWxOpenComponentService();
            
            // 构造请求 body
            String requestBody = String.format(
                    "{\"component_appid\":\"%s\",\"component_appsecret\":\"%s\",\"component_verify_ticket\":\"%s\"}",
                    componentAppId, componentAppSecret, componentVerifyTicket);
            
            // 调用微信接口
            String responseJson = componentService.post(
                    WxOpenComponentService.API_COMPONENT_TOKEN_URL, 
                    requestBody);
            
            log.debug("[WxJavaWeChatOpenPlatformClient] getComponentAccessToken response: {}", responseJson);
            
            // 解析响应
            JsonNode jsonNode = objectMapper.readTree(responseJson);
            
            // 检查是否有错误
            if (jsonNode.has("errcode") && jsonNode.get("errcode").asInt() != 0) {
                int errcode = jsonNode.get("errcode").asInt();
                String errmsg = jsonNode.has("errmsg") ? jsonNode.get("errmsg").asText() : "unknown error";
                log.error("[WxJavaWeChatOpenPlatformClient] getComponentAccessToken 失败, errcode={}, errmsg={}", 
                        errcode, errmsg);
                result.setErrcode(errcode);
                result.setErrmsg(errmsg);
                return result;
            }
            
            // 解析成功响应
            if (jsonNode.has("component_access_token")) {
                String token = jsonNode.get("component_access_token").asText();
                int expiresIn = jsonNode.has("expires_in") ? jsonNode.get("expires_in").asInt() : 7200;
                
                result.setComponentAccessToken(token);
                result.setExpiresIn(expiresIn);
                result.setErrcode(0);
                
                log.info("[WxJavaWeChatOpenPlatformClient] getComponentAccessToken 成功, expiresIn={}s", expiresIn);
            } else {
                log.error("[WxJavaWeChatOpenPlatformClient] getComponentAccessToken 响应缺少 component_access_token");
                result.setErrcode(-1);
                result.setErrmsg("response missing component_access_token");
            }
            
        } catch (WxErrorException e) {
            log.error("[WxJavaWeChatOpenPlatformClient] getComponentAccessToken 失败, errcode={}, errmsg={}", 
                    e.getError().getErrorCode(), e.getError().getErrorMsg());
            result.setErrcode(e.getError().getErrorCode());
            result.setErrmsg(e.getError().getErrorMsg());
        } catch (Exception e) {
            log.error("[WxJavaWeChatOpenPlatformClient] getComponentAccessToken 异常", e);
            result.setErrcode(-1);
            result.setErrmsg("Internal error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 创建预授权码 pre_auth_code。
     * <p>
     * 调用微信开放平台接口：POST https://api.weixin.qq.com/cgi-bin/component/api_create_preauthcode
     * </p>
     *
     * @param componentAccessToken 第三方平台 component_access_token
     * @return 预授权码结果
     */
    @Override
    public PreAuthCodeResult createPreAuthCode(String componentAccessToken) {
        log.info("[WxJavaWeChatOpenPlatformClient] createPreAuthCode");
        
        PreAuthCodeResult result = new PreAuthCodeResult();
        result.setObtainedAt(Instant.now());
        
        try {
            WxOpenComponentService componentService = wxOpenService.getWxOpenComponentService();
            String componentAppId = wxOpenService.getWxOpenConfigStorage().getComponentAppId();
            
            // 构造请求 body
            String requestBody = String.format("{\"component_appid\":\"%s\"}", componentAppId);
            
            // 调用微信接口（携带 component_access_token）
            String responseJson = componentService.post(
                    WxOpenComponentService.API_CREATE_PREAUTHCODE_URL,
                    requestBody,
                    "component_access_token",
                    componentAccessToken);
            
            log.debug("[WxJavaWeChatOpenPlatformClient] createPreAuthCode response: {}", responseJson);
            
            // 解析响应
            JsonNode jsonNode = objectMapper.readTree(responseJson);
            
            // 检查是否有错误
            if (jsonNode.has("errcode") && jsonNode.get("errcode").asInt() != 0) {
                int errcode = jsonNode.get("errcode").asInt();
                String errmsg = jsonNode.has("errmsg") ? jsonNode.get("errmsg").asText() : "unknown error";
                log.error("[WxJavaWeChatOpenPlatformClient] createPreAuthCode 失败, errcode={}, errmsg={}", 
                        errcode, errmsg);
                result.setErrcode(errcode);
                result.setErrmsg(errmsg);
                return result;
            }
            
            // 解析成功响应
            if (jsonNode.has("pre_auth_code")) {
                String preAuthCode = jsonNode.get("pre_auth_code").asText();
                int expiresIn = jsonNode.has("expires_in") ? jsonNode.get("expires_in").asInt() : 600;
                
                result.setPreAuthCode(preAuthCode);
                result.setExpiresIn(expiresIn);
                result.setErrcode(0);
                
                log.info("[WxJavaWeChatOpenPlatformClient] createPreAuthCode 成功, expiresIn={}s", expiresIn);
            } else {
                log.error("[WxJavaWeChatOpenPlatformClient] createPreAuthCode 响应缺少 pre_auth_code");
                result.setErrcode(-1);
                result.setErrmsg("response missing pre_auth_code");
            }
            
        } catch (WxErrorException e) {
            log.error("[WxJavaWeChatOpenPlatformClient] createPreAuthCode 失败, errcode={}, errmsg={}", 
                    e.getError().getErrorCode(), e.getError().getErrorMsg());
            result.setErrcode(e.getError().getErrorCode());
            result.setErrmsg(e.getError().getErrorMsg());
        } catch (Exception e) {
            log.error("[WxJavaWeChatOpenPlatformClient] createPreAuthCode 异常", e);
            result.setErrcode(-1);
            result.setErrmsg("Internal error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 使用授权码查询授权信息。
     * <p>
     * 调用微信开放平台接口：POST https://api.weixin.qq.com/cgi-bin/component/api_query_auth
     * </p>
     *
     * @param componentAccessToken 第三方平台 component_access_token
     * @param authorizationCode    授权码
     * @return 授权信息查询结果
     */
    @Override
    public QueryAuthResult queryAuth(String componentAccessToken, String authorizationCode) {
        log.info("[WxJavaWeChatOpenPlatformClient] queryAuth, authCode={}", 
                maskToken(authorizationCode));
        
        QueryAuthResult result = new QueryAuthResult();
        
        try {
            WxOpenComponentService componentService = wxOpenService.getWxOpenComponentService();
            String componentAppId = wxOpenService.getWxOpenConfigStorage().getComponentAppId();
            
            // 构造请求 body
            String requestBody = String.format(
                    "{\"component_appid\":\"%s\",\"authorization_code\":\"%s\"}",
                    componentAppId, authorizationCode);
            
            // 调用微信接口
            String responseJson = componentService.post(
                    WxOpenComponentService.API_QUERY_AUTH_URL,
                    requestBody,
                    "component_access_token",
                    componentAccessToken);
            
            log.debug("[WxJavaWeChatOpenPlatformClient] queryAuth response: {}", responseJson);
            
            // 解析响应
            JsonNode jsonNode = objectMapper.readTree(responseJson);
            
            // 检查是否有错误
            if (jsonNode.has("errcode") && jsonNode.get("errcode").asInt() != 0) {
                int errcode = jsonNode.get("errcode").asInt();
                String errmsg = jsonNode.has("errmsg") ? jsonNode.get("errmsg").asText() : "unknown error";
                log.error("[WxJavaWeChatOpenPlatformClient] queryAuth 失败, errcode={}, errmsg={}", 
                        errcode, errmsg);
                result.setErrcode(errcode);
                result.setErrmsg(errmsg);
                return result;
            }
            
            // 解析 authorization_info
            if (jsonNode.has("authorization_info")) {
                JsonNode authInfo = jsonNode.get("authorization_info");
                
                AuthorizationInfo authorizationInfo = new AuthorizationInfo();
                
                if (authInfo.has("authorizer_appid")) {
                    authorizationInfo.setAuthorizerAppid(authInfo.get("authorizer_appid").asText());
                }
                if (authInfo.has("authorizer_access_token")) {
                    authorizationInfo.setAuthorizerAccessToken(authInfo.get("authorizer_access_token").asText());
                }
                if (authInfo.has("expires_in")) {
                    authorizationInfo.setExpiresIn(authInfo.get("expires_in").asInt());
                }
                if (authInfo.has("authorizer_refresh_token")) {
                    authorizationInfo.setAuthorizerRefreshToken(authInfo.get("authorizer_refresh_token").asText());
                }
                
                // 解析 func_info
                if (authInfo.has("func_info")) {
                    JsonNode funcInfoArray = authInfo.get("func_info");
                    List<Integer> funcCategories = new ArrayList<>();
                    for (JsonNode funcNode : funcInfoArray) {
                        if (funcNode.has("funcscope_category")) {
                            JsonNode categoryNode = funcNode.get("funcscope_category");
                            if (categoryNode.has("id")) {
                                funcCategories.add(categoryNode.get("id").asInt());
                            }
                        }
                    }
                    authorizationInfo.setFuncInfo(funcCategories);
                    result.setFuncScopeCategories(funcCategories);
                    result.setFuncInfoCount(funcCategories.size());
                }
                
                // 设置到 result
                result.setAuthorizationInfo(authorizationInfo);
                result.setAuthorizerAppId(authorizationInfo.getAuthorizerAppid());
                result.setAuthorizerRefreshToken(authorizationInfo.getAuthorizerRefreshToken());
                result.setErrcode(0);
                
                log.info("[WxJavaWeChatOpenPlatformClient] queryAuth 成功, authorizerAppId={}", 
                        result.getAuthorizerAppId());
            } else {
                log.error("[WxJavaWeChatOpenPlatformClient] queryAuth 响应缺少 authorization_info");
                result.setErrcode(-1);
                result.setErrmsg("response missing authorization_info");
            }
            
        } catch (WxErrorException e) {
            log.error("[WxJavaWeChatOpenPlatformClient] queryAuth 失败, errcode={}, errmsg={}", 
                    e.getError().getErrorCode(), e.getError().getErrorMsg());
            result.setErrcode(e.getError().getErrorCode());
            result.setErrmsg(e.getError().getErrorMsg());
        } catch (Exception e) {
            log.error("[WxJavaWeChatOpenPlatformClient] queryAuth 异常", e);
            result.setErrcode(-1);
            result.setErrmsg("Internal error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取已授权方（小程序）的基本信息。
     * <p>
     * 调用微信开放平台接口：POST https://api.weixin.qq.com/cgi-bin/component/api_get_authorizer_info
     * </p>
     *
     * @param componentAccessToken 第三方平台 component_access_token
     * @param authorizerAppId      授权方 appid
     * @return 授权方基本信息
     */
    @Override
    public Optional<AuthorizerInfoResult> getAuthorizerInfo(
            String componentAccessToken,
            String authorizerAppId) {
        log.info("[WxJavaWeChatOpenPlatformClient] getAuthorizerInfo, authorizerAppId={}", 
                authorizerAppId);
        
        try {
            WxOpenComponentService componentService = wxOpenService.getWxOpenComponentService();
            String componentAppId = wxOpenService.getWxOpenConfigStorage().getComponentAppId();
            
            // 构造请求 body
            String requestBody = String.format(
                    "{\"component_appid\":\"%s\",\"authorizer_appid\":\"%s\"}",
                    componentAppId, authorizerAppId);
            
            // 调用微信接口
            String responseJson = componentService.post(
                    WxOpenComponentService.API_GET_AUTHORIZER_INFO_URL,
                    requestBody,
                    "component_access_token",
                    componentAccessToken);
            
            log.debug("[WxJavaWeChatOpenPlatformClient] getAuthorizerInfo response: {}", responseJson);
            
            // 解析响应
            JsonNode jsonNode = objectMapper.readTree(responseJson);
            
            AuthorizerInfoResult result = new AuthorizerInfoResult();
            result.setAuthorizerAppId(authorizerAppId);
            
            // 检查是否有错误
            if (jsonNode.has("errcode") && jsonNode.get("errcode").asInt() != 0) {
                int errcode = jsonNode.get("errcode").asInt();
                String errmsg = jsonNode.has("errmsg") ? jsonNode.get("errmsg").asText() : "unknown error";
                log.error("[WxJavaWeChatOpenPlatformClient] getAuthorizerInfo 失败, errcode={}, errmsg={}", 
                        errcode, errmsg);
                result.setErrcode(errcode);
                result.setErrmsg(errmsg);
                return Optional.of(result);
            }
            
            // 解析 authorizer_info
            if (jsonNode.has("authorizer_info")) {
                JsonNode authInfo = jsonNode.get("authorizer_info");
                
                if (authInfo.has("nick_name")) {
                    result.setNickName(authInfo.get("nick_name").asText());
                }
                if (authInfo.has("principal_name")) {
                    result.setPrincipalName(authInfo.get("principal_name").asText());
                }
                if (authInfo.has("head_img")) {
                    result.setHeadImg(authInfo.get("head_img").asText());
                }
                if (authInfo.has("signature")) {
                    result.setSignature(authInfo.get("signature").asText());
                }
                
                // 解析 verify_type_info
                if (authInfo.has("verify_type_info")) {
                    JsonNode verifyTypeInfo = authInfo.get("verify_type_info");
                    if (verifyTypeInfo.has("id")) {
                        result.setVerifyType(verifyTypeInfo.get("id").asInt());
                    }
                }
                
                // 解析 service_type_info (主体类型)
                if (authInfo.has("service_type_info")) {
                    JsonNode serviceTypeInfo = authInfo.get("service_type_info");
                    if (serviceTypeInfo.has("id")) {
                        result.setPrincipalType(serviceTypeInfo.get("id").asInt());
                    }
                }
            }
            
            result.setErrcode(0);
            
            log.info("[WxJavaWeChatOpenPlatformClient] getAuthorizerInfo 成功, nickName={}", 
                    result.getNickName());
            
            return Optional.of(result);
            
        } catch (WxErrorException e) {
            log.error("[WxJavaWeChatOpenPlatformClient] getAuthorizerInfo 失败, errcode={}, errmsg={}", 
                    e.getError().getErrorCode(), e.getError().getErrorMsg());
            
            AuthorizerInfoResult result = new AuthorizerInfoResult();
            result.setAuthorizerAppId(authorizerAppId);
            result.setErrcode(e.getError().getErrorCode());
            result.setErrmsg(e.getError().getErrorMsg());
            return Optional.of(result);
            
        } catch (Exception e) {
            log.error("[WxJavaWeChatOpenPlatformClient] getAuthorizerInfo 异常", e);
            return Optional.empty();
        }
    }

    /**
     * 刷新授权方的接口调用令牌（authorizer_access_token）。
     * <p>
     * 调用微信开放平台接口：POST https://api.weixin.qq.com/cgi-bin/component/api_authorizer_token
     * </p>
     *
     * @param componentAccessToken   第三方平台 component_access_token
     * @param componentAppId         第三方平台 appid
     * @param authorizerAppId        授权方 appid
     * @param authorizerRefreshToken 授权方的刷新令牌
     * @return 刷新结果
     */
    @Override
    public RefreshAuthorizerTokenResult refreshAuthorizerToken(
            String componentAccessToken,
            String componentAppId,
            String authorizerAppId,
            String authorizerRefreshToken) {
        log.info("[WxJavaWeChatOpenPlatformClient] refreshAuthorizerToken, authorizerAppId={}", 
                authorizerAppId);
        
        try {
            WxOpenComponentService componentService = wxOpenService.getWxOpenComponentService();
            
            // 构造请求 body
            String requestBody = String.format(
                    "{\"component_appid\":\"%s\",\"authorizer_appid\":\"%s\",\"authorizer_refresh_token\":\"%s\"}",
                    componentAppId, authorizerAppId, authorizerRefreshToken);
            
            // 调用微信接口
            String responseJson = componentService.post(
                    WxOpenComponentService.API_AUTHORIZER_TOKEN_URL,
                    requestBody,
                    "component_access_token",
                    componentAccessToken);
            
            log.debug("[WxJavaWeChatOpenPlatformClient] refreshAuthorizerToken response: {}", responseJson);
            
            // 解析响应
            JsonNode jsonNode = objectMapper.readTree(responseJson);
            
            // 检查是否有错误
            if (jsonNode.has("errcode") && jsonNode.get("errcode").asInt() != 0) {
                int errcode = jsonNode.get("errcode").asInt();
                String errmsg = jsonNode.has("errmsg") ? jsonNode.get("errmsg").asText() : "unknown error";
                log.error("[WxJavaWeChatOpenPlatformClient] refreshAuthorizerToken 失败, errcode={}, errmsg={}", 
                        errcode, errmsg);
                throw new IllegalStateException(
                        String.format("刷新 authorizer_access_token 失败, errcode=%d, errmsg=%s", errcode, errmsg));
            }
            
            // 解析成功响应
            String newAccessToken = jsonNode.has("authorizer_access_token") 
                    ? jsonNode.get("authorizer_access_token").asText() 
                    : null;
            String newRefreshToken = jsonNode.has("authorizer_refresh_token") 
                    ? jsonNode.get("authorizer_refresh_token").asText() 
                    : authorizerRefreshToken; // 如果微信没返回新的，使用旧的
            int expiresIn = jsonNode.has("expires_in") 
                    ? jsonNode.get("expires_in").asInt() 
                    : 7200;
            
            if (newAccessToken == null) {
                log.error("[WxJavaWeChatOpenPlatformClient] refreshAuthorizerToken 响应缺少 authorizer_access_token");
                throw new IllegalStateException("response missing authorizer_access_token");
            }
            
            RefreshAuthorizerTokenResult result = RefreshAuthorizerTokenResult.builder()
                    .authorizerAccessToken(newAccessToken)
                    .authorizerRefreshToken(newRefreshToken)
                    .expiresInSeconds(expiresIn)
                    .build();
            
            log.info("[WxJavaWeChatOpenPlatformClient] refreshAuthorizerToken 成功, authorizerAppId={}, expiresIn={}s", 
                    authorizerAppId, expiresIn);
            
            return result;
            
        } catch (WxErrorException e) {
            log.error("[WxJavaWeChatOpenPlatformClient] refreshAuthorizerToken 失败, errcode={}, errmsg={}", 
                    e.getError().getErrorCode(), e.getError().getErrorMsg());
            throw new IllegalStateException(
                    String.format("刷新 authorizer_access_token 失败, errcode=%d, errmsg=%s",
                            e.getError().getErrorCode(), e.getError().getErrorMsg()), e);
        } catch (Exception e) {
            log.error("[WxJavaWeChatOpenPlatformClient] refreshAuthorizerToken 异常", e);
            throw new IllegalStateException("刷新 authorizer_access_token 异常: " + e.getMessage(), e);
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
     * 脱敏 Token（只显示前 8 位）
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 8) + "...";
    }
}
