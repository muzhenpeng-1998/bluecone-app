package com.bluecone.app.infra.security.token;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Token 配置，来自 bluecone.security.token.*。
 */
@Data
@Component
@ConfigurationProperties(prefix = "bluecone.security.token")
public class TokenProperties {

    /**
     * HS256 密钥，需配置为足够长度的随机字符串。
     */
    private String secret;

    /**
     * Access Token 过期时间（分钟）。
     */
    private long accessTokenTtlMinutes = 15;

    /**
     * Refresh Token 过期时间（天）。
     */
    private long refreshTokenTtlDays = 7;

    /**
     * JWT issuer。
     */
    private String issuer = "bluecone-app";
}
