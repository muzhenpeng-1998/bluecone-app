package com.bluecone.app.payment.infrastructure.wechatpay;

import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 微信支付 V3 服务商模式配置类。
 * <p>
 * 构建 WxPayService 单例 Bean，用于服务商下单和回调验签解密。
 * </p>
 */
@Configuration
public class WxJavaWeChatPayConfiguration {

    private static final Logger log = LoggerFactory.getLogger(WxJavaWeChatPayConfiguration.class);

    /**
     * 构建 WxPayService Bean（仅当 enabled=true 时启用）。
     */
    @Bean
    @ConditionalOnProperty(prefix = "bluecone.wechat.pay", name = "enabled", havingValue = "true")
    public WxPayService wxPayService(BlueconeWeChatPayProperties properties) {
        log.info("[WxJavaWeChatPayConfiguration] 初始化微信支付服务商模式，spMchId={}", properties.getSpMchId());

        validateProperties(properties);

        WxPayConfig config = new WxPayConfig();
        config.setAppId(properties.getSpAppId());
        config.setMchId(properties.getSpMchId());
        config.setApiV3Key(properties.getApiV3Key());
        config.setCertSerialNo(properties.getCertSerialNo());

        // 设置商户私钥（优先使用文件路径，其次使用字符串）
        if (StringUtils.hasText(properties.getPrivateKeyPath())) {
            try {
                String privateKeyContent = new String(Files.readAllBytes(Paths.get(properties.getPrivateKeyPath())));
                config.setPrivateKeyContent(privateKeyContent.getBytes());
                log.info("[WxJavaWeChatPayConfiguration] 从文件加载商户私钥，path={}", properties.getPrivateKeyPath());
            } catch (IOException e) {
                throw new IllegalStateException("无法读取商户私钥文件: " + properties.getPrivateKeyPath(), e);
            }
        } else if (StringUtils.hasText(properties.getPrivateKeyString())) {
            config.setPrivateKeyContent(properties.getPrivateKeyString().getBytes());
            log.info("[WxJavaWeChatPayConfiguration] 从配置字符串加载商户私钥");
        } else {
            throw new IllegalStateException("商户私钥配置缺失，请配置 privateKeyPath 或 privateKeyString");
        }

        WxPayService wxPayService = new WxPayServiceImpl();
        wxPayService.setConfig(config);

        log.info("[WxJavaWeChatPayConfiguration] 微信支付服务商模式初始化完成");
        return wxPayService;
    }

    /**
     * 校验配置必填项。
     */
    private void validateProperties(BlueconeWeChatPayProperties properties) {
        if (!StringUtils.hasText(properties.getSpAppId())) {
            throw new IllegalStateException("微信支付配置缺失：spAppId");
        }
        if (!StringUtils.hasText(properties.getSpMchId())) {
            throw new IllegalStateException("微信支付配置缺失：spMchId");
        }
        if (!StringUtils.hasText(properties.getApiV3Key())) {
            throw new IllegalStateException("微信支付配置缺失：apiV3Key");
        }
        if (!StringUtils.hasText(properties.getCertSerialNo())) {
            throw new IllegalStateException("微信支付配置缺失：certSerialNo");
        }
    }
}

