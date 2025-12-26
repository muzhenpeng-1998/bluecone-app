package com.bluecone.app.wechat.facade.openplatform;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 预授权链接生成结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatPreAuthUrlResult {

    /**
     * 预授权链接（完整 URL）
     */
    private String preAuthUrl;

    /**
     * 预授权码（用于调试）
     */
    private String preAuthCode;

    /**
     * 预授权码过期时间（秒）
     */
    private Integer expiresIn;
}

