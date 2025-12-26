package com.bluecone.app.wechat.facade.miniapp;

import com.bluecone.app.infra.wechat.openplatform.WechatAuthorizedAppService;
import com.bluecone.app.infra.wechat.openplatform.WechatComponentCredentialService;
import com.bluecone.app.wechat.route.WeChatRouteContext;
import com.bluecone.app.wechat.route.WeChatRouteService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.open.api.WxOpenComponentService;
import me.chanjar.weixin.open.api.WxOpenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 基于 WxJava SDK 的微信小程序 Facade 实现。
 * <p>
 * 唯一使用 WxJava open/miniapp SDK 的地方，封装多租户路由、token 管理、异常处理。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class WxJavaWeChatMiniAppFacade implements WeChatMiniAppFacade {

    private static final Logger log = LoggerFactory.getLogger(WxJavaWeChatMiniAppFacade.class);

    private final WeChatRouteService routeService;
    private final WxOpenService wxOpenService;
    private final WechatComponentCredentialService componentCredentialService;
    private final WechatAuthorizedAppService authorizedAppService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public WeChatMiniAppLoginResult code2Session(WeChatMiniAppCode2SessionCommand command) {
        log.info("[WxJavaWeChatMiniAppFacade] code2Session, tenantId={}, storeId={}",
                command.getTenantId(), command.getStoreId());

        // 1. 路由到 authorizerAppId
        WeChatRouteContext ctx = routeService.resolve(command.getTenantId(), command.getStoreId());
        String authorizerAppId = ctx.getAuthorizerAppId();

        log.debug("[WxJavaWeChatMiniAppFacade] resolved authorizerAppId={}***",
                maskAppId(authorizerAppId));

        try {
            // 2. 获取 component_access_token
            String componentAccessToken = componentCredentialService.getValidComponentAccessToken();

            // 3. 获取 component_appid
            String componentAppId = wxOpenService.getWxOpenConfigStorage().getComponentAppId();

            // 4. 拼接 URL（第三方平台模式的 jscode2session）
            String url = String.format(
                    WxOpenComponentService.MINIAPP_JSCODE_2_SESSION,
                    authorizerAppId, command.getCode(), componentAppId, componentAccessToken);

            log.debug("[WxJavaWeChatMiniAppFacade] code2Session URL: {}",
                    url.replaceAll("component_access_token=[^&]+", "component_access_token=***"));

            // 5. 调用微信接口
            WxOpenComponentService componentService = wxOpenService.getWxOpenComponentService();
            String responseJson = componentService.get(url, null);

            log.debug("[WxJavaWeChatMiniAppFacade] code2Session response: {}", responseJson);

            // 6. 解析响应
            JsonNode jsonNode = objectMapper.readTree(responseJson);

            // 检查错误
            if (jsonNode.has("errcode") && jsonNode.get("errcode").asInt() != 0) {
                int errcode = jsonNode.get("errcode").asInt();
                String errmsg = jsonNode.has("errmsg") ? jsonNode.get("errmsg").asText() : "unknown error";
                log.error("[WxJavaWeChatMiniAppFacade] code2Session 失败, tenantId={}, errcode={}, errmsg={}",
                        command.getTenantId(), errcode, errmsg);
                throw new IllegalStateException(
                        String.format("code2Session 失败, errcode=%d, errmsg=%s", errcode, errmsg));
            }

            // 7. 构造结果（脱敏日志）
            WeChatMiniAppLoginResult result = WeChatMiniAppLoginResult.builder()
                    .openId(jsonNode.has("openid") ? jsonNode.get("openid").asText() : null)
                    .unionId(jsonNode.has("unionid") ? jsonNode.get("unionid").asText() : null)
                    .sessionKey(jsonNode.has("session_key") ? jsonNode.get("session_key").asText() : null)
                    .build();

            log.info("[WxJavaWeChatMiniAppFacade] code2Session 成功, tenantId={}, openId={}***",
                    command.getTenantId(), maskOpenId(result.getOpenId()));

            return result;

        } catch (WxErrorException e) {
            log.error("[WxJavaWeChatMiniAppFacade] code2Session WxErrorException, tenantId={}, errcode={}, errmsg={}",
                    command.getTenantId(), e.getError().getErrorCode(), e.getError().getErrorMsg());
            throw new IllegalStateException(
                    String.format("code2Session 失败, errcode=%d, errmsg=%s",
                            e.getError().getErrorCode(), e.getError().getErrorMsg()), e);
        } catch (Exception e) {
            log.error("[WxJavaWeChatMiniAppFacade] code2Session 异常, tenantId={}",
                    command.getTenantId(), e);
            throw new IllegalStateException("code2Session 异常: " + e.getMessage(), e);
        }
    }

    @Override
    public WeChatMiniAppPhoneResult getPhoneNumber(WeChatMiniAppPhoneCommand command) {
        log.info("[WxJavaWeChatMiniAppFacade] getPhoneNumber, tenantId={}, storeId={}",
                command.getTenantId(), command.getStoreId());

        // 1. 路由到 authorizerAppId
        WeChatRouteContext ctx = routeService.resolve(command.getTenantId(), command.getStoreId());
        String authorizerAppId = ctx.getAuthorizerAppId();

        log.debug("[WxJavaWeChatMiniAppFacade] resolved authorizerAppId={}***",
                maskAppId(authorizerAppId));

        try {
            // 2. 获取或刷新 authorizer_access_token
            String authorizerAccessToken = authorizedAppService
                    .getOrRefreshAuthorizerAccessToken(authorizerAppId);

            log.debug("[WxJavaWeChatMiniAppFacade] 获取到 authorizer_access_token={}...",
                    maskToken(authorizerAccessToken));

            // 3. 构造请求 URL
            String url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token="
                    + authorizerAccessToken;

            // 4. 构造请求 body
            String requestBody = String.format("{\"code\":\"%s\"}", command.getPhoneCode());

            // 5. 调用微信接口
            WxOpenComponentService componentService = wxOpenService.getWxOpenComponentService();
            String responseJson = componentService.post(url, requestBody);

            log.debug("[WxJavaWeChatMiniAppFacade] getPhoneNumber response: {}", responseJson);

            // 6. 解析响应
            JsonNode jsonNode = objectMapper.readTree(responseJson);

            // 检查错误
            if (jsonNode.has("errcode") && jsonNode.get("errcode").asInt() != 0) {
                int errcode = jsonNode.get("errcode").asInt();
                String errmsg = jsonNode.has("errmsg") ? jsonNode.get("errmsg").asText() : "unknown error";
                log.error("[WxJavaWeChatMiniAppFacade] getPhoneNumber 失败, tenantId={}, errcode={}, errmsg={}",
                        command.getTenantId(), errcode, errmsg);
                throw new IllegalStateException(
                        String.format("获取手机号失败, errcode=%d, errmsg=%s", errcode, errmsg));
            }

            // 7. 解析 phone_info
            WeChatMiniAppPhoneResult result = WeChatMiniAppPhoneResult.builder().build();

            if (jsonNode.has("phone_info")) {
                JsonNode phoneInfo = jsonNode.get("phone_info");
                if (phoneInfo.has("phoneNumber")) {
                    result.setPhoneNumber(phoneInfo.get("phoneNumber").asText());
                }
                if (phoneInfo.has("countryCode")) {
                    result.setCountryCode(phoneInfo.get("countryCode").asText());
                }
                if (phoneInfo.has("purePhoneNumber")) {
                    result.setPurePhoneNumber(phoneInfo.get("purePhoneNumber").asText());
                }
            }

            log.info("[WxJavaWeChatMiniAppFacade] getPhoneNumber 成功, tenantId={}, phone={}***",
                    command.getTenantId(), maskPhone(result.getPhoneNumber()));

            return result;

        } catch (WxErrorException e) {
            log.error("[WxJavaWeChatMiniAppFacade] getPhoneNumber WxErrorException, tenantId={}, errcode={}, errmsg={}",
                    command.getTenantId(), e.getError().getErrorCode(), e.getError().getErrorMsg());
            throw new IllegalStateException(
                    String.format("获取手机号失败, errcode=%d, errmsg=%s",
                            e.getError().getErrorCode(), e.getError().getErrorMsg()), e);
        } catch (Exception e) {
            log.error("[WxJavaWeChatMiniAppFacade] getPhoneNumber 异常, tenantId={}",
                    command.getTenantId(), e);
            throw new IllegalStateException("获取手机号异常: " + e.getMessage(), e);
        }
    }

    /**
     * 脱敏 AppID（只显示前 6 位）
     */
    private String maskAppId(String appId) {
        if (appId == null || appId.length() <= 6) {
            return "***";
        }
        return appId.substring(0, 6);
    }

    /**
     * 脱敏 OpenID（只显示前 8 位）
     */
    private String maskOpenId(String openId) {
        if (openId == null || openId.length() <= 8) {
            return "***";
        }
        return openId.substring(0, 8);
    }

    /**
     * 脱敏 Token（只显示前 8 位）
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 8);
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

