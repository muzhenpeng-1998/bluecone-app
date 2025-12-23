package com.bluecone.app.api.integration.wechat;

import com.bluecone.app.infra.wechat.openplatform.WechatComponentCredentialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * 微信开放平台 Ticket 推送回调接口。
 * <p>
 * 微信开放平台每 10 分钟推送一次 component_verify_ticket，
 * 第三方平台需要接收并保存，用于后续获取 component_access_token。
 * </p>
 * <p>
 * 回调 URL 示例：https://yourdomain.com/api/wechat/open/callback/ticket
 * </p>
 * <p>
 * 注意事项：
 * 1. 此接口必须在安全配置中 permitAll，不需要登录认证
 * 2. 必须进行微信签名校验，防止伪造请求
 * 3. 如果配置了消息加密，需要解密 XML 消息体
 * </p>
 */
@Tag(name = "🔌 第三方集成 > 微信相关 > 微信开放平台回调", description = "微信开放平台 Ticket 推送回调")
@RestController
@RequestMapping("/api/wechat/open/callback")
@RequiredArgsConstructor
public class WechatOpenTicketCallbackController {

    private static final Logger log = LoggerFactory.getLogger(WechatOpenTicketCallbackController.class);

    private final WechatComponentCredentialService credentialService;

    @Value("${wechat.open-platform.component-app-id:}")
    private String componentAppId;

    @Value("${wechat.open-platform.component-token:}")
    private String componentToken;

    @Value("${wechat.open-platform.component-aes-key:}")
    private String componentAesKey;

    /**
     * 接收微信开放平台推送的 component_verify_ticket。
     * <p>
     * GET 请求：用于微信服务器验证 URL 有效性（首次配置时）
     * POST 请求：接收实际的 ticket 推送消息
     * </p>
     *
     * @param signature 微信加密签名，用于校验消息来自微信服务器
     * @param timestamp 时间戳
     * @param nonce     随机数
     * @param echostr   随机字符串（仅 GET 请求，用于 URL 验证）
     * @param encryptType 加密类型（aes 表示加密消息）
     * @param msgSignature 消息签名（加密消息时使用）
     * @param requestBody POST 请求体（XML 格式）
     * @return 成功返回 "success" 或 echostr
     */
    @Operation(summary = "接收微信开放平台 Ticket 推送", description = "接收 component_verify_ticket 并保存到数据库")
    @GetMapping("/ticket")
    public String verifyUrl(
            @RequestParam(value = "msg_signature", required = false) String msgSignature,
            @RequestParam(value = "timestamp", required = false) String timestamp,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestParam(value = "echostr", required = false) String echostr) {
        
        log.info("[WechatOpenTicket] GET request for URL verification, msgSignature={}, timestamp={}, nonce={}",
                msgSignature, timestamp, nonce);

        // 验证签名并解密 echostr
        if (!StringUtils.hasText(msgSignature) || !StringUtils.hasText(timestamp) 
                || !StringUtils.hasText(nonce) || !StringUtils.hasText(echostr)) {
            log.warn("[WechatOpenTicket] GET request missing required parameters");
            return "error";
        }

        try {
            // 使用 WxJava 的解密工具验证签名并解密 echostr
            me.chanjar.weixin.common.util.crypto.WxCryptUtil cryptUtil = 
                    new me.chanjar.weixin.common.util.crypto.WxCryptUtil(
                            componentToken, componentAesKey, componentAppId);
            
            // 验证签名并解密
            String decryptedEchostr = cryptUtil.decrypt(msgSignature, timestamp, nonce, echostr);
            
            log.info("[WechatOpenTicket] URL verification successful, returning decrypted echostr");
            return decryptedEchostr;
        } catch (Exception e) {
            log.error("[WechatOpenTicket] Failed to decrypt echostr", e);
            return "error";
        }
    }

    @PostMapping(value = "/ticket", produces = MediaType.TEXT_PLAIN_VALUE)
    public String receiveTicket(
            @RequestParam(value = "msg_signature", required = false) String msgSignature,
            @RequestParam(value = "timestamp", required = false) String timestamp,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestBody String requestBody) {

        log.info("[WechatOpenTicket] POST request received, msgSignature={}, timestamp={}, nonce={}",
                msgSignature, timestamp, nonce);
        log.debug("[WechatOpenTicket] Request body (encrypted): {}", requestBody);

        try {
            // 1. 验证签名并解密消息
            if (!StringUtils.hasText(msgSignature) || !StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce)) {
                log.warn("[WechatOpenTicket] POST request missing required parameters");
                return "error";
            }

            if (!StringUtils.hasText(componentToken) || !StringUtils.hasText(componentAesKey) 
                    || !StringUtils.hasText(componentAppId)) {
                log.error("[WechatOpenTicket] WeChat component configuration is incomplete");
                return "error";
            }

            // 2. 使用 WxJava 的解密工具解密消息
            me.chanjar.weixin.common.util.crypto.WxCryptUtil cryptUtil = 
                    new me.chanjar.weixin.common.util.crypto.WxCryptUtil(
                            componentToken, componentAesKey, componentAppId);
            
            // 从 XML 中提取 Encrypt 字段
            String encryptedMsg = extractEncryptedMessage(requestBody);
            if (!StringUtils.hasText(encryptedMsg)) {
                log.warn("[WechatOpenTicket] Failed to extract Encrypt from XML");
                return "error";
            }
            
            // 解密消息
            String decryptedXml = cryptUtil.decrypt(msgSignature, timestamp, nonce, encryptedMsg);
            log.debug("[WechatOpenTicket] Decrypted XML: {}", decryptedXml);

            // 3. 解析明文 XML，提取 InfoType 和对应内容
            String infoType = extractInfoType(decryptedXml);
            log.info("[WechatOpenTicket] InfoType: {}", infoType);

            if ("component_verify_ticket".equals(infoType)) {
                // 提取 ComponentVerifyTicket
                String componentVerifyTicket = extractComponentVerifyTicket(decryptedXml);
                if (!StringUtils.hasText(componentVerifyTicket)) {
                    log.warn("[WechatOpenTicket] Failed to extract ComponentVerifyTicket from XML");
                    return "error";
                }

                // 保存到数据库
                credentialService.saveOrUpdateVerifyTicket(componentVerifyTicket);
                log.info("[WechatOpenTicket] ComponentVerifyTicket saved successfully: {}", 
                        componentVerifyTicket.substring(0, Math.min(20, componentVerifyTicket.length())) + "...");
            } else if ("unauthorized".equals(infoType)) {
                // 处理取消授权事件
                String authorizerAppId = extractAuthorizerAppId(decryptedXml);
                if (StringUtils.hasText(authorizerAppId)) {
                    log.info("[WechatOpenTicket] Received unauthorized event, authorizerAppId={}", authorizerAppId);
                    // 调用应用服务处理取消授权
                    // wechatOpenCallbackAppService.handleUnauthorized(authorizerAppId);
                    log.warn("[WechatOpenTicket] Unauthorized event handling not implemented yet");
                } else {
                    log.warn("[WechatOpenTicket] Failed to extract AuthorizerAppid from unauthorized event");
                }
            } else {
                log.info("[WechatOpenTicket] Received other InfoType: {}, ignoring", infoType);
            }

            return "success";

        } catch (Exception e) {
            log.error("[WechatOpenTicket] Failed to process ticket callback", e);
            return "error";
        }
    }

    /**
     * 从加密的 XML 消息体中提取 Encrypt 字段。
     * <p>
     * XML 格式示例：
     * <xml>
     *   <ToUserName><![CDATA[gh_xxx]]></ToUserName>
     *   <Encrypt><![CDATA[encrypted_content...]]></Encrypt>
     * </xml>
     * </p>
     */
    private String extractEncryptedMessage(String xmlContent) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 防止 XXE 攻击
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));
            
            Element root = doc.getDocumentElement();
            return getElementText(root, "Encrypt");

        } catch (Exception e) {
            log.error("[WechatOpenTicket] Failed to extract Encrypt from XML", e);
            return null;
        }
    }

    /**
     * 从明文 XML 消息体中提取 InfoType。
     */
    private String extractInfoType(String xmlContent) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));
            
            Element root = doc.getDocumentElement();
            return getElementText(root, "InfoType");

        } catch (Exception e) {
            log.error("[WechatOpenTicket] Failed to extract InfoType from XML", e);
            return null;
        }
    }

    /**
     * 从明文 XML 消息体中提取 AuthorizerAppid（用于 unauthorized 事件）。
     */
    private String extractAuthorizerAppId(String xmlContent) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));
            
            Element root = doc.getDocumentElement();
            return getElementText(root, "AuthorizerAppid");

        } catch (Exception e) {
            log.error("[WechatOpenTicket] Failed to extract AuthorizerAppid from XML", e);
            return null;
        }
    }

    /**
     * 从明文 XML 消息体中提取 ComponentVerifyTicket。
     * <p>
     * XML 格式示例：
     * <xml>
     *   <AppId><![CDATA[wx1234567890abcdef]]></AppId>
     *   <CreateTime>1234567890</CreateTime>
     *   <InfoType><![CDATA[component_verify_ticket]]></InfoType>
     *   <ComponentVerifyTicket><![CDATA[ticket@@@...]]></ComponentVerifyTicket>
     * </xml>
     * </p>
     */
    private String extractComponentVerifyTicket(String xmlContent) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 防止 XXE 攻击
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));
            
            Element root = doc.getDocumentElement();
            String ticket = getElementText(root, "ComponentVerifyTicket");
            log.info("[WechatOpenTicket] Extracted ComponentVerifyTicket from XML");
            return ticket;

        } catch (Exception e) {
            log.error("[WechatOpenTicket] Failed to parse XML", e);
            return null;
        }
    }

    /**
     * 获取 XML 元素的文本内容。
     */
    private String getElementText(Element parent, String tagName) {
        org.w3c.dom.NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }
}

