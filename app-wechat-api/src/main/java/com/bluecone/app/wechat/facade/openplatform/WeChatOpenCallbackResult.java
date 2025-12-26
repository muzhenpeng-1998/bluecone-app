package com.bluecone.app.wechat.facade.openplatform;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信开放平台回调处理结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatOpenCallbackResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 消息（用于返回给微信，如 "success"）
     */
    private String message;

    /**
     * 事件类型（component_verify_ticket / authorized / updateauthorized / unauthorized）
     */
    private String infoType;

    /**
     * 授权方 AppID（如果是授权相关事件）
     */
    private String authorizerAppId;

    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;
}

