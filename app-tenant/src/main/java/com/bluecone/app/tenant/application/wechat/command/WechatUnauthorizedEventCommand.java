package com.bluecone.app.tenant.application.wechat.command;

/**
 * 微信小程序取消授权事件命令。
 * <p>承载微信开放平台 unauthorized 事件中的核心字段。</p>
 */
public record WechatUnauthorizedEventCommand(
        // 被取消授权的小程序 appid
        String authorizerAppid) {
}

