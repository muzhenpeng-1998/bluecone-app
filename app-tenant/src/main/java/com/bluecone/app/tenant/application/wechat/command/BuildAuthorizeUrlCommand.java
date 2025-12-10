package com.bluecone.app.tenant.application.wechat.command;

/**
 * 构建微信开放平台授权 URL 的命令。
 */
public record BuildAuthorizeUrlCommand(
        /**
         * 入驻会话 token。
         * 用于在授权回跳时识别当前入驻流程与租户/门店。
         */
        String sessionToken,
        /**
         * 授权完成后的回调 URI（H5 回跳地址）。
         * 将在其上追加 sessionToken 参数并作为 redirect_uri 传给微信开放平台。
         */
        String redirectUri) {
}

