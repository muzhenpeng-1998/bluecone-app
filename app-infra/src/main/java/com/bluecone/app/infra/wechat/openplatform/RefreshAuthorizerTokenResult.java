package com.bluecone.app.infra.wechat.openplatform;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 刷新授权方接口调用令牌结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshAuthorizerTokenResult {

    /**
     * 授权方接口调用令牌
     */
    private String authorizerAccessToken;

    /**
     * 授权方刷新令牌（用于下次刷新）
     */
    private String authorizerRefreshToken;

    /**
     * 有效期（秒）
     */
    private Integer expiresInSeconds;
}

