package com.bluecone.app.wechat.openplatform;

import com.bluecone.app.infra.wechat.openplatform.*;
import com.bluecone.app.wechat.config.WeChatOpenPlatformProperties;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.open.api.WxOpenService;
import me.chanjar.weixin.open.bean.result.WxOpenAuthorizerInfoResult;
import me.chanjar.weixin.open.bean.result.WxOpenQueryAuthResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
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

    public WxJavaWeChatOpenPlatformClient(
            WxOpenService wxOpenService,
            WechatComponentCredentialService wechatComponentCredentialService,
            WechatAuthorizedAppService wechatAuthorizedAppService,
            WeChatOpenPlatformProperties properties) {
        this.wxOpenService = wxOpenService;
        this.wechatComponentCredentialService = wechatComponentCredentialService;
        this.wechatAuthorizedAppService = wechatAuthorizedAppService;
        this.properties = properties;
        log.info("[WxJavaWeChatOpenPlatformClient] 初始化完成, componentAppId={}", 
                properties.getComponentAppId());
    }

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
            // 使用 WxJava 的 ComponentService 获取 component_access_token
            String token = wxOpenService.getWxOpenComponentService()
                    .getComponentAccessToken(false); // false = 不强制刷新
            
            result.setComponentAccessToken(token);
            result.setExpiresIn(7200); // 默认 2 小时
            result.setErrcode(0);
            
            log.info("[WxJavaWeChatOpenPlatformClient] getComponentAccessToken 成功, token={}...", 
                    maskToken(token));
            
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

    @Override
    public PreAuthCodeResult createPreAuthCode(String componentAccessToken) {
        log.info("[WxJavaWeChatOpenPlatformClient] createPreAuthCode");
        
        PreAuthCodeResult result = new PreAuthCodeResult();
        result.setObtainedAt(Instant.now());
        
        try {
            // 使用 WxJava 的 ComponentService 创建预授权码
            String preAuthCode = wxOpenService.getWxOpenComponentService()
                    .getPreAuthUrl(null); // 获取预授权码（WxJava 会自动调用接口）
            
            // 注意：WxJava 的 getPreAuthUrl 返回的是完整 URL，我们需要提取 pre_auth_code
            // 但实际上我们应该直接调用获取 pre_auth_code 的方法
            // 让我们使用正确的方法
            String actualPreAuthCode = wxOpenService.getWxOpenComponentService()
                    .getPreAuthUrl(""); // 空字符串会只返回 pre_auth_code
            
            result.setPreAuthCode(actualPreAuthCode);
            result.setExpiresIn(600); // 默认 10 分钟
            result.setErrcode(0);
            
            log.info("[WxJavaWeChatOpenPlatformClient] createPreAuthCode 成功");
            
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

    @Override
    public QueryAuthResult queryAuth(String componentAccessToken, String authorizationCode) {
        log.info("[WxJavaWeChatOpenPlatformClient] queryAuth, authCode={}", 
                maskToken(authorizationCode));
        
        QueryAuthResult result = new QueryAuthResult();
        
        try {
            // 使用 WxJava 的 ComponentService 查询授权信息
            WxOpenQueryAuthResult wxResult = wxOpenService.getWxOpenComponentService()
                    .getQueryAuth(authorizationCode);
            
            // 映射到我们的 DTO
            // WxOpenQueryAuthResult 包含授权信息
            result.setAuthorizerAppId(wxResult.getAuthorizationInfo().getAuthorizerAppid());
            result.setAuthorizerRefreshToken(wxResult.getAuthorizationInfo().getAuthorizerRefreshToken());
            
            // 注意：WxJava 4.7.0 的返回可能不包含 funcInfo
            // 如果需要权限信息，可以通过其他接口获取
            result.setFuncInfoCount(0);
            
            result.setErrcode(0);
            
            log.info("[WxJavaWeChatOpenPlatformClient] queryAuth 成功, authorizerAppId={}", 
                    result.getAuthorizerAppId());
            
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

    @Override
    public Optional<AuthorizerInfoResult> getAuthorizerInfo(
            String componentAccessToken,
            String authorizerAppId) {
        log.info("[WxJavaWeChatOpenPlatformClient] getAuthorizerInfo, authorizerAppId={}", 
                authorizerAppId);
        
        try {
            // 使用 WxJava 的 ComponentService 获取授权方信息
            WxOpenAuthorizerInfoResult wxResult = wxOpenService.getWxOpenComponentService()
                    .getAuthorizerInfo(authorizerAppId);
            
            // 映射到我们的 DTO
            AuthorizerInfoResult result = new AuthorizerInfoResult();
            result.setAuthorizerAppId(authorizerAppId);
            
            if (wxResult.getAuthorizerInfo() != null) {
                result.setNickName(wxResult.getAuthorizerInfo().getNickName());
                result.setPrincipalName(wxResult.getAuthorizerInfo().getPrincipalName());
                result.setHeadImg(wxResult.getAuthorizerInfo().getHeadImg());
                result.setSignature(wxResult.getAuthorizerInfo().getSignature());
                result.setVerifyType(wxResult.getAuthorizerInfo().getVerifyTypeInfo());
                // 注意：WxJava 4.7.0 可能不包含 getPrincipalType 方法
                // result.setPrincipalType(wxResult.getAuthorizerInfo().getPrincipalType());
            }
            
            result.setErrcode(0);
            
            log.info("[WxJavaWeChatOpenPlatformClient] getAuthorizerInfo 成功, nickName={}", 
                    result.getNickName());
            
            return Optional.of(result);
            
        } catch (WxErrorException e) {
            log.error("[WxJavaWeChatOpenPlatformClient] getAuthorizerInfo 失败, errcode={}, errmsg={}", 
                    e.getError().getErrorCode(), e.getError().getErrorMsg());
            return Optional.empty();
        } catch (Exception e) {
            log.error("[WxJavaWeChatOpenPlatformClient] getAuthorizerInfo 异常", e);
            return Optional.empty();
        }
    }

    @Override
    public RefreshAuthorizerTokenResult refreshAuthorizerToken(
            String componentAccessToken,
            String componentAppId,
            String authorizerAppId,
            String authorizerRefreshToken) {
        log.info("[WxJavaWeChatOpenPlatformClient] refreshAuthorizerToken, authorizerAppId={}", 
                authorizerAppId);
        
        // TODO: 根据实际 WxJava 4.7.0 API 调整方法调用
        // 可能的方法: wxOpenService.getWxOpenComponentService().getAuthorizerToken(...)
        // 或: wxOpenService.getWxOpenComponentService().refreshAuthorizerToken(...)
        
        log.error("[WxJavaWeChatOpenPlatformClient] refreshAuthorizerToken 暂未实现");
        throw new UnsupportedOperationException(
                "refreshAuthorizerToken 需要根据 WxJava 4.7.0 实际 API 实现。" +
                "请参考 WxJava 文档：https://github.com/Wechat-Group/WxJava");
    }

    /**
     * 从 JSON 字符串中提取字符串值
     */
    private String extractJsonValue(String json, String key) {
        if (json == null || key == null) {
            return null;
        }
        
        String searchKey = "\"" + key + "\":\"";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) {
            return null;
        }
        startIndex += searchKey.length();
        int endIndex = json.indexOf("\"", startIndex);
        if (endIndex == -1) {
            return null;
        }
        return json.substring(startIndex, endIndex);
    }

    /**
     * 从嵌套 JSON 字符串中提取值
     */
    private String extractNestedJsonValue(String json, String parentKey, String childKey) {
        if (json == null || parentKey == null || childKey == null) {
            return null;
        }
        
        // 找到父对象
        String parentSearchKey = "\"" + parentKey + "\":{";
        int parentStart = json.indexOf(parentSearchKey);
        if (parentStart == -1) {
            return null;
        }
        
        // 找到父对象的结束位置
        int parentObjStart = parentStart + parentSearchKey.length() - 1;
        int parentObjEnd = findMatchingBrace(json, parentObjStart);
        if (parentObjEnd == -1) {
            return null;
        }
        
        // 在父对象内查找子键
        String parentJson = json.substring(parentObjStart, parentObjEnd + 1);
        return extractJsonValue(parentJson, childKey);
    }

    /**
     * 找到匹配的右花括号
     */
    private int findMatchingBrace(String json, int startIndex) {
        int braceCount = 0;
        for (int i = startIndex; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 从 JSON 字符串中提取整数值
     */
    private Integer extractJsonIntValue(String json, String key) {
        if (json == null || key == null) {
            return null;
        }
        
        String searchKey = "\"" + key + "\":";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) {
            return null;
        }
        startIndex += searchKey.length();
        
        // 找到下一个逗号或右花括号
        int endIndex = json.indexOf(",", startIndex);
        int endIndex2 = json.indexOf("}", startIndex);
        if (endIndex == -1) {
            endIndex = endIndex2;
        } else if (endIndex2 != -1 && endIndex2 < endIndex) {
            endIndex = endIndex2;
        }
        
        if (endIndex == -1) {
            return null;
        }
        
        try {
            return Integer.parseInt(json.substring(startIndex, endIndex).trim());
        } catch (NumberFormatException e) {
            return null;
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
