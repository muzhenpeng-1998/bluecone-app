package com.bluecone.app.gateway.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import lombok.Data;

import java.util.Arrays;
import java.util.List;

/**
 * External configuration for the embedded API gateway.
 */
@Data
@ConfigurationProperties(prefix = "bluecone.gateway")
public class ApiGatewayProperties {

    private static final Logger log = LoggerFactory.getLogger(ApiGatewayProperties.class);
    private static final String DEFAULT_DEV_SECRET = "bluecone-signature-dev";

    @Autowired
    private Environment environment;

    /**
     * Toggle gateway routing. When disabled, controller can short-circuit.
     */
    private boolean enabled = true;

    /**
     * Shared secret for simple HMAC signature verification.
     */
    private String signatureSecret;

    /**
     * Allowed clock skew for signature timestamp.
     */
    private long signatureToleranceSeconds = 300;

    /**
     * 启动时校验：生产环境必须配置强密钥。
     */
    @PostConstruct
    public void validateProductionSecret() {
        // 检查是否为生产环境
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean isProd = activeProfiles.contains("prod") || activeProfiles.contains("production");

        if (!isProd) {
            log.info("[ApiGatewayProperties] 非生产环境，跳过 Gateway 签名密钥校验");
            // 非生产环境：如果未配置，使用默认开发密钥
            if (!StringUtils.hasText(signatureSecret)) {
                log.warn("[ApiGatewayProperties] Gateway 签名密钥未配置，使用默认开发密钥");
                signatureSecret = DEFAULT_DEV_SECRET;
            }
            return;
        }

        log.info("[ApiGatewayProperties] 生产环境，开始校验 Gateway 签名密钥");

        // 校验 1：密钥不能为空
        if (!StringUtils.hasText(signatureSecret)) {
            throw new IllegalStateException(
                    "生产环境必须配置 Gateway 签名密钥。" +
                    "请通过环境变量 GATEWAY_SIGNATURE_SECRET 注入 bluecone.gateway.signature-secret 配置。" +
                    "密钥要求：至少 32 字符的随机字符串，定期轮换。");
        }

        // 校验 2：密钥不能是默认开发密钥
        if (DEFAULT_DEV_SECRET.equals(signatureSecret)) {
            throw new IllegalStateException(
                    "生产环境禁止使用默认开发密钥。" +
                    "请通过环境变量 GATEWAY_SIGNATURE_SECRET 注入强密钥。" +
                    "密钥要求：至少 32 字符的随机字符串，定期轮换。");
        }

        // 校验 3：密钥长度至少 32 字符
        if (signatureSecret.length() < 32) {
            throw new IllegalStateException(
                    "生产环境 Gateway 签名密钥长度不足。" +
                    "当前长度：" + signatureSecret.length() + " 字符，要求至少 32 字符。" +
                    "请通过环境变量 GATEWAY_SIGNATURE_SECRET 注入足够长度的强密钥。");
        }

        log.info("[ApiGatewayProperties] 生产环境 Gateway 签名密钥校验通过，密钥长度：{} 字符", 
                signatureSecret.length());
    }
}
