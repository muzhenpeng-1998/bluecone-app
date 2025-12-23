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
            @RequestParam(value = "signature", required = false) String signature,
            @RequestParam(value = "timestamp", required = false) String timestamp,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestParam(value = "echostr", required = false) String echostr) {
        
        log.info("[WechatOpenTicket] GET request for URL verification, signature={}, timestamp={}, nonce={}, echostr={}",
                signature, timestamp, nonce, echostr);

        // 验证签名
        if (!verifySignature(signature, timestamp, nonce)) {
            log.warn("[WechatOpenTicket] GET request signature verification failed");
            return "error";
        }

        // 首次配置时，微信服务器会发送 GET 请求验证 URL，需要原样返回 echostr
        log.info("[WechatOpenTicket] URL verification successful, returning echostr");
        return echostr;
    }

    @PostMapping(value = "/ticket", produces = MediaType.TEXT_PLAIN_VALUE)
    public String receiveTicket(
            @RequestParam(value = "signature", required = false) String signature,
            @RequestParam(value = "timestamp", required = false) String timestamp,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestParam(value = "encrypt_type", required = false) String encryptType,
            @RequestParam(value = "msg_signature", required = false) String msgSignature,
            @RequestBody String requestBody) {

        log.info("[WechatOpenTicket] POST request received, signature={}, timestamp={}, nonce={}, encryptType={}, msgSignature={}",
                signature, timestamp, nonce, encryptType, msgSignature);
        log.debug("[WechatOpenTicket] Request body: {}", requestBody);

        try {
            // 1. 验证签名
            if (!verifySignature(signature, timestamp, nonce)) {
                log.warn("[WechatOpenTicket] POST request signature verification failed");
                return "error";
            }

            // 2. 解析 XML 消息体
            String xmlContent = requestBody;
            
            // 如果是加密消息，需要先解密（这里简化处理，实际应使用 WxJava 的解密工具）
            if ("aes".equalsIgnoreCase(encryptType) && StringUtils.hasText(componentAesKey)) {
                log.info("[WechatOpenTicket] Message is encrypted, need to decrypt");
                // TODO: 使用 WxJava 的 WxCryptUtil 解密消息
                // 当前简化实现，假设消息未加密或已在网关层解密
            }

            // 3. 解析 XML，提取 ComponentVerifyTicket
            String componentVerifyTicket = extractComponentVerifyTicket(xmlContent);
            if (!StringUtils.hasText(componentVerifyTicket)) {
                log.warn("[WechatOpenTicket] Failed to extract ComponentVerifyTicket from XML");
                return "error";
            }

            // 4. 保存到数据库
            credentialService.saveOrUpdateVerifyTicket(componentVerifyTicket);
            log.info("[WechatOpenTicket] ComponentVerifyTicket saved successfully: {}", 
                    componentVerifyTicket.substring(0, Math.min(20, componentVerifyTicket.length())) + "...");

            return "success";

        } catch (Exception e) {
            log.error("[WechatOpenTicket] Failed to process ticket callback", e);
            return "error";
        }
    }

    /**
     * 验证微信签名。
     * <p>
     * 签名算法：
     * 1. 将 token、timestamp、nonce 三个参数进行字典序排序
     * 2. 将三个参数字符串拼接成一个字符串进行 SHA1 加密
     * 3. 开发者获得加密后的字符串可与 signature 对比，标识该请求来源于微信
     * </p>
     */
    private boolean verifySignature(String signature, String timestamp, String nonce) {
        if (!StringUtils.hasText(signature) || !StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce)) {
            log.warn("[WechatOpenTicket] Signature verification failed: missing parameters");
            return false;
        }

        if (!StringUtils.hasText(componentToken)) {
            log.error("[WechatOpenTicket] componentToken is not configured, cannot verify signature");
            return false;
        }

        try {
            // 1. 字典序排序
            String[] params = {componentToken, timestamp, nonce};
            Arrays.sort(params);

            // 2. 拼接字符串
            String concatenated = String.join("", params);

            // 3. SHA1 加密
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(concatenated.getBytes(StandardCharsets.UTF_8));
            
            // 4. 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            String calculatedSignature = hexString.toString();
            boolean valid = calculatedSignature.equalsIgnoreCase(signature);
            
            if (!valid) {
                log.warn("[WechatOpenTicket] Signature mismatch: expected={}, actual={}", calculatedSignature, signature);
            }
            
            return valid;

        } catch (Exception e) {
            log.error("[WechatOpenTicket] Failed to verify signature", e);
            return false;
        }
    }

    /**
     * 从 XML 消息体中提取 ComponentVerifyTicket。
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
            String infoType = getElementText(root, "InfoType");
            
            if (!"component_verify_ticket".equals(infoType)) {
                log.warn("[WechatOpenTicket] InfoType is not component_verify_ticket: {}", infoType);
                return null;
            }
            
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

