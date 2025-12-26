package com.bluecone.app.wechat.facade.openplatform;

import com.bluecone.app.infra.wechat.openplatform.*;
import com.bluecone.app.wechat.config.WeChatOpenPlatformProperties;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.common.util.crypto.WxCryptUtil;
import me.chanjar.weixin.open.api.WxOpenComponentService;
import me.chanjar.weixin.open.api.WxOpenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;

/**
 * 基于 WxJava SDK 的微信开放平台 Facade 实现。
 * <p>
 * 唯一使用 WxJava open SDK 的地方，封装预授权链接生成、回调处理等功能。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class WxJavaWeChatOpenPlatformFacade implements WeChatOpenPlatformFacade {

    private static final Logger log = LoggerFactory.getLogger(WxJavaWeChatOpenPlatformFacade.class);

    private final WxOpenService wxOpenService;
    private final WeChatOpenPlatformProperties properties;
    private final WechatComponentCredentialService componentCredentialService;
    private final WeChatOpenPlatformClient weChatOpenPlatformClient;

    @Override
    public WeChatPreAuthUrlResult buildPreAuthUrl(WeChatPreAuthUrlCommand command) {
        log.info("[WxJavaWeChatOpenPlatformFacade] buildPreAuthUrl, tenantId={}, storeId={}",
                command.getTenantId(), command.getStoreId());

        try {
            // 1. 获取或刷新 component_access_token
            String componentAccessToken = componentCredentialService.getValidComponentAccessToken();

            // 2. 创建 pre_auth_code
            PreAuthCodeResult preAuthResult = weChatOpenPlatformClient.createPreAuthCode(componentAccessToken);

            if (!preAuthResult.isSuccess() || !StringUtils.hasText(preAuthResult.getPreAuthCode())) {
                log.error("[WxJavaWeChatOpenPlatformFacade] createPreAuthCode 失败, errcode={}, errmsg={}",
                        preAuthResult.getErrcode(), preAuthResult.getErrmsg());
                throw new IllegalStateException("创建预授权码失败: " + preAuthResult.getErrmsg());
            }

            String preAuthCode = preAuthResult.getPreAuthCode();
            String componentAppId = properties.getComponentAppId();

            // 3. 构造 redirect_uri（使用命令中的）
            String redirectUri = command.getRedirectUri();

            if (!StringUtils.hasText(redirectUri)) {
                throw new IllegalStateException("redirectUri 不能为空");
            }

            // 4. 拼接授权链接
            // https://mp.weixin.qq.com/cgi-bin/componentloginpage?component_appid=XXX&pre_auth_code=XXX&redirect_uri=XXX&auth_type=1
            StringBuilder urlBuilder = new StringBuilder("https://mp.weixin.qq.com/cgi-bin/componentloginpage");
            urlBuilder.append("?component_appid=").append(componentAppId);
            urlBuilder.append("&pre_auth_code=").append(preAuthCode);
            urlBuilder.append("&redirect_uri=").append(urlEncode(redirectUri));
            urlBuilder.append("&auth_type=1"); // 1=仅小程序

            // 5. 添加自定义参数（如 sessionToken）
            if (StringUtils.hasText(command.getCustomParam())) {
                urlBuilder.append("&").append(command.getCustomParam());
            }

            String preAuthUrl = urlBuilder.toString();

            log.info("[WxJavaWeChatOpenPlatformFacade] buildPreAuthUrl 成功, tenantId={}, preAuthCode={}...",
                    command.getTenantId(), maskToken(preAuthCode));

            return WeChatPreAuthUrlResult.builder()
                    .preAuthUrl(preAuthUrl)
                    .preAuthCode(preAuthCode)
                    .expiresIn(preAuthResult.getExpiresIn())
                    .build();

        } catch (Exception e) {
            log.error("[WxJavaWeChatOpenPlatformFacade] buildPreAuthUrl 异常, tenantId={}",
                    command.getTenantId(), e);
            throw new IllegalStateException("生成预授权链接失败: " + e.getMessage(), e);
        }
    }

    @Override
    public WeChatOpenCallbackResult handleCallback(WeChatOpenCallbackCommand command) {
        log.info("[WxJavaWeChatOpenPlatformFacade] handleCallback, msgSignature={}, timestamp={}",
                command.getMsgSignature(), command.getTimestamp());

        try {
            // 1. 验签 + 解密
            String decryptedXml = decryptMessage(command);

            if (!StringUtils.hasText(decryptedXml)) {
                log.error("[WxJavaWeChatOpenPlatformFacade] 解密失败");
                return WeChatOpenCallbackResult.builder()
                        .success(false)
                        .message("decrypt failed")
                        .errorMessage("消息解密失败")
                        .build();
            }

            log.debug("[WxJavaWeChatOpenPlatformFacade] 解密成功，明文长度={}", decryptedXml.length());

            // 2. 解析 InfoType 和 AuthorizerAppid
            ParsedCallbackEvent event = parseCallbackEvent(decryptedXml);

            if (event == null || !StringUtils.hasText(event.infoType)) {
                log.error("[WxJavaWeChatOpenPlatformFacade] 解析事件失败");
                return WeChatOpenCallbackResult.builder()
                        .success(false)
                        .message("parse failed")
                        .errorMessage("事件解析失败")
                        .build();
            }

            log.info("[WxJavaWeChatOpenPlatformFacade] 解析事件：InfoType={}, AuthorizerAppid={}",
                    event.infoType, event.authorizerAppid);

            // 3. 幂等检查（使用 InfoType + createTime + appid + bodyHash）
            String idempotencyKey = buildIdempotencyKey(event, command.getRawBody());
            if (isDuplicate(idempotencyKey)) {
                log.info("[WxJavaWeChatOpenPlatformFacade] 幂等命中，跳过处理, key={}", idempotencyKey);
                return WeChatOpenCallbackResult.builder()
                        .success(true)
                        .message("success")
                        .infoType(event.infoType)
                        .authorizerAppId(event.authorizerAppid)
                        .build();
            }

            // 4. 根据 InfoType 分发处理
            handleEventByType(event);

            // 5. 记录幂等
            recordIdempotency(idempotencyKey);

            return WeChatOpenCallbackResult.builder()
                    .success(true)
                    .message("success")
                    .infoType(event.infoType)
                    .authorizerAppId(event.authorizerAppid)
                    .build();

        } catch (Exception e) {
            log.error("[WxJavaWeChatOpenPlatformFacade] handleCallback 异常", e);
            return WeChatOpenCallbackResult.builder()
                    .success(false)
                    .message("error")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 解密消息
     */
    private String decryptMessage(WeChatOpenCallbackCommand command) {
        try {
            String componentToken = properties.getComponentToken();
            String componentAesKey = properties.getComponentAesKey();
            String componentAppId = properties.getComponentAppId();

            if (!StringUtils.hasText(componentToken) || !StringUtils.hasText(componentAesKey)
                    || !StringUtils.hasText(componentAppId)) {
                log.error("[WxJavaWeChatOpenPlatformFacade] 配置缺失：componentToken/componentAesKey/componentAppId");
                return null;
            }

            WxCryptUtil cryptUtil = new WxCryptUtil(componentToken, componentAesKey, componentAppId);
            return cryptUtil.decrypt(command.getRawBody());

        } catch (Exception e) {
            log.error("[WxJavaWeChatOpenPlatformFacade] 消息解密失败", e);
            return null;
        }
    }

    /**
     * 解析回调事件
     */
    private ParsedCallbackEvent parseCallbackEvent(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            Element root = doc.getDocumentElement();

            ParsedCallbackEvent event = new ParsedCallbackEvent();
            event.infoType = getElementText(root, "InfoType");
            event.authorizerAppid = getElementText(root, "AuthorizerAppid");
            event.createTime = getElementText(root, "CreateTime");
            event.componentVerifyTicket = getElementText(root, "ComponentVerifyTicket");

            return event;

        } catch (Exception e) {
            log.error("[WxJavaWeChatOpenPlatformFacade] XML 解析失败", e);
            return null;
        }
    }

    /**
     * 从 XML Element 中提取文本内容
     */
    private String getElementText(Element parent, String tagName) {
        try {
            org.w3c.dom.NodeList nodeList = parent.getElementsByTagName(tagName);
            if (nodeList.getLength() > 0) {
                org.w3c.dom.Node node = nodeList.item(0);
                return node.getTextContent();
            }
        } catch (Exception e) {
            log.debug("[WxJavaWeChatOpenPlatformFacade] 提取 {} 失败", tagName, e);
        }
        return null;
    }

    /**
     * 根据 InfoType 分发处理
     * <p>
     * 注意：此方法只处理 component_verify_ticket，其他事件（authorized/unauthorized）
     * 应该由 app-tenant 的 WechatOpenCallbackAppService 处理。
     * 为了避免循环依赖，这里只做最基础的处理。
     * </p>
     */
    private void handleEventByType(ParsedCallbackEvent event) {
        switch (event.infoType) {
            case "component_verify_ticket":
                // 保存 ticket
                if (StringUtils.hasText(event.componentVerifyTicket)) {
                    componentCredentialService.saveOrUpdateVerifyTicket(event.componentVerifyTicket);
                    log.info("[WxJavaWeChatOpenPlatformFacade] component_verify_ticket 已保存");
                }
                break;

            case "authorized":
            case "updateauthorized":
                // 授权/更新授权：只记录日志，实际处理由 app-tenant 完成
                if (StringUtils.hasText(event.authorizerAppid)) {
                    log.info("[WxJavaWeChatOpenPlatformFacade] 收到 {} 事件，AuthorizerAppid={}（需要由 app-tenant 处理）",
                            event.infoType, event.authorizerAppid);
                }
                break;

            case "unauthorized":
                // 取消授权：只记录日志，实际处理由 app-tenant 完成
                if (StringUtils.hasText(event.authorizerAppid)) {
                    log.info("[WxJavaWeChatOpenPlatformFacade] 收到 unauthorized 事件，AuthorizerAppid={}（需要由 app-tenant 处理）",
                            event.authorizerAppid);
                }
                break;

            default:
                log.info("[WxJavaWeChatOpenPlatformFacade] 未处理的 InfoType: {}", event.infoType);
                break;
        }
    }

    /**
     * 构造幂等 key
     */
    private String buildIdempotencyKey(ParsedCallbackEvent event, String rawBody) {
        try {
            String source = event.infoType + "_" + event.createTime + "_" + event.authorizerAppid + "_"
                    + hashBody(rawBody);
            return "wechat_callback_" + source;
        } catch (Exception e) {
            log.error("[WxJavaWeChatOpenPlatformFacade] 构造幂等 key 失败", e);
            return "wechat_callback_" + System.currentTimeMillis();
        }
    }

    /**
     * 计算 body hash
     */
    private String hashBody(String body) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(body.getBytes());
            return Base64.getEncoder().encodeToString(hash).substring(0, 16);
        } catch (Exception e) {
            return "hash_error";
        }
    }

    /**
     * 检查是否重复（简单内存实现，生产环境应使用 Redis）
     */
    private boolean isDuplicate(String key) {
        // TODO: 使用 Redis 实现幂等检查
        // 当前简化实现：不做幂等检查
        return false;
    }

    /**
     * 记录幂等
     */
    private void recordIdempotency(String key) {
        // TODO: 使用 Redis 记录幂等 key，设置过期时间（如 24 小时）
        // 当前简化实现：不记录
    }

    /**
     * URL 编码
     */
    private String urlEncode(String str) {
        try {
            return java.net.URLEncoder.encode(str, "UTF-8");
        } catch (Exception e) {
            return str;
        }
    }

    /**
     * 脱敏 Token
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 8);
    }

    /**
     * 解析后的回调事件
     */
    private static class ParsedCallbackEvent {
        String infoType;
        String authorizerAppid;
        String createTime;
        String componentVerifyTicket;
    }
}

