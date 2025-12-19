package com.bluecone.app.application.wechat;

import com.bluecone.app.api.advice.NoApiResponseWrap;
import com.bluecone.app.tenant.application.wechat.WechatOpenCallbackAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信开放平台“授权事件接收 URL”统一入口。
 * <p>
 * 当前版本仅接收微信开放平台回调，请求参数与原始加密 XML 透传给应用服务做后续处理，
 * 并按微信约定返回字符串 "success" 防止重复推送。
 * </p>
 */
@RestController
@RequestMapping("/api/wechat/open")
@NoApiResponseWrap
public class WechatOpenPlatformCallbackController {

    private static final Logger log = LoggerFactory.getLogger(WechatOpenPlatformCallbackController.class);

    private final WechatOpenCallbackAppService wechatOpenCallbackAppService;

    public WechatOpenPlatformCallbackController(WechatOpenCallbackAppService wechatOpenCallbackAppService) {
        this.wechatOpenCallbackAppService = wechatOpenCallbackAppService;
    }

    /**
     * 微信开放平台回调入口（授权/取消授权等事件）。
     * <p>
     * 目前仅记录基础日志并将原始参数透传给应用服务，后续会在服务层实现消息解密与事件分发。
     * </p>
     *
     * @return 固定返回 "success" 表示接收成功
     */
    @PostMapping("/callback")
    public String handleCallback(
            @RequestParam(name = "signature", required = false) String signature,
            @RequestParam(name = "timestamp", required = false) String timestamp,
            @RequestParam(name = "nonce", required = false) String nonce,
            @RequestParam(name = "msg_signature", required = false) String msgSignature,
            @RequestBody String requestBody
    ) {
        log.info("[WechatOpenCallback] HTTP callback received, signature={}, timestamp={}, nonce={}, msgSignature={}",
                signature, timestamp, nonce, msgSignature);

        wechatOpenCallbackAppService.handleRawCallback(signature, timestamp, nonce, msgSignature, requestBody);

        return "success";
    }
}

