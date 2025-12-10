package com.bluecone.app.infra.wechat;

import lombok.Data;

/**
 * fastregisterweapp 返回结果封装。
 */
@Data
public class WeChatFastRegisterResult {

    /**
     * 注册成功后的小程序 appid（有的场景会立即下发）。
     */
    private String appId;

    /**
     * 微信返回的原始 JSON。
     */
    private String rawBody;
}

