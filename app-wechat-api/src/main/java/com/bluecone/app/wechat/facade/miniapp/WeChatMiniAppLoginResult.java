package com.bluecone.app.wechat.facade.miniapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小程序登录结果。
 * <p>
 * 只返回业务需要的字段：openId/unionId/sessionKey（可选）。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatMiniAppLoginResult {

    /**
     * 用户唯一标识（小程序维度）
     */
    private String openId;

    /**
     * 用户唯一标识（开放平台维度，可能为空）
     */
    private String unionId;

    /**
     * 会话密钥（可选，用于解密敏感数据）
     * <p>
     * 注意：sessionKey 属于敏感信息，建议只在服务端内部使用，不返回给客户端。
     * 如果不需要解密功能，可以不返回此字段。
     * </p>
     */
    private String sessionKey;
}

