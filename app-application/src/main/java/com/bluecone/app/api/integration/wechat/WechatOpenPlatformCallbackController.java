package com.bluecone.app.api.integration.wechat;

import com.bluecone.app.api.advice.NoApiResponseWrap;
import com.bluecone.app.wechat.facade.openplatform.WeChatOpenCallbackCommand;
import com.bluecone.app.wechat.facade.openplatform.WeChatOpenCallbackResult;
import com.bluecone.app.wechat.facade.openplatform.WeChatOpenPlatformFacade;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信开放平台"授权事件接收 URL"统一入口。
 * <p>
 * Phase 3 版本：极薄 Controller，只转发 headers/body/path 到 wechat facade。
 * 不做验签解密、不做 InfoType switch，所有逻辑在 facade 层完成。
 * </p>
 */
@Tag(name = "🔌 第三方集成 > 微信相关 > 微信开放平台回调", description = "微信开放平台事件回调接口")
@RestController
@RequestMapping("/api/wechat/open")
@NoApiResponseWrap
@RequiredArgsConstructor
public class WechatOpenPlatformCallbackController {

    private static final Logger log = LoggerFactory.getLogger(WechatOpenPlatformCallbackController.class);

    private final WeChatOpenPlatformFacade weChatOpenPlatformFacade;

    /**
     * 微信开放平台回调入口（授权/取消授权等事件）。
     * <p>
     * 极薄实现：只转发参数到 wechat facade 处理。
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
        log.info("[WechatOpenCallback] HTTP callback received, msgSignature={}, timestamp={}",
                msgSignature, timestamp);

        // 构造命令并调用 facade
        WeChatOpenCallbackCommand command = WeChatOpenCallbackCommand.builder()
                .signature(signature)
                .timestamp(timestamp)
                .nonce(nonce)
                .msgSignature(msgSignature)
                .rawBody(requestBody)
                .build();

        WeChatOpenCallbackResult result = weChatOpenPlatformFacade.handleCallback(command);

        // 返回微信要求的响应
        return result.isSuccess() ? "success" : result.getMessage();
    }
}

