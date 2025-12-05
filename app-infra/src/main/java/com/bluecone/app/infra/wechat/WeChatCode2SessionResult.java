package com.bluecone.app.infra.wechat;

import lombok.Data;

/**
 * 微信 code2session 结果。
 */
@Data
public class WeChatCode2SessionResult {

    private String openId;

    private String unionId;

    private String sessionKey;
}
