package com.bluecone.app.wechat.pay.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 微信支付 V3 服务商模式配置属性。
 * <p>
 * 服务商密钥信息不进数据库，通过配置文件或环境变量注入。
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.pay.partner")
public class WeChatPayPartnerProperties {

    private static final Logger log = LoggerFactory.getLogger(WeChatPayPartnerProperties.class);

    /**
     * 是否启用微信支付（默认 false，本地开发时可关闭使用 stub）。
     */
    private boolean enabled = false;

    /**
     * 服务商应用 AppID（sp_appid）。
     */
    private String spAppId;

    /**
     * 服务商商户号（sp_mchid）。
     */
    private String spMchId;

    /**
     * 微信支付 V3 API 密钥（32位，用于回调解密）。
     */
    private String apiV3Key;

    /**
     * 商户证书序列号。
     */
    private String certSerialNo;

    /**
     * 商户私钥路径（apiclient_key.pem 文件路径，优先使用）。
     */
    private String privateKeyPath;

    /**
     * 商户私钥字符串（如果不使用文件路径，可直接配置私钥内容）。
     */
    private String privateKeyString;

    /**
     * 默认回调地址（当 PaymentChannelConfig.notifyUrl 为空时使用）。
     */
    private String defaultNotifyUrl;

    /**
     * 启动时校验：当 enabled=true 时，必须配置所有必填字段。
     */
    @PostConstruct
    public void validate() {
        if (!enabled) {
            log.info("[WeChatPayPartnerProperties] 微信支付服务商模式未启用");
            return;
        }

        log.info("[WeChatPayPartnerProperties] 微信支付服务商模式已启用，开始校验配置");

        // 校验必填字段
        if (!StringUtils.hasText(spAppId)) {
            throw new IllegalStateException(
                    "微信支付服务商模式已启用，但 wechat.pay.partner.sp-app-id 未配置。" +
                    "请通过环境变量 WECHAT_PAY_SP_APP_ID 注入服务商应用 AppID。");
        }

        if (!StringUtils.hasText(spMchId)) {
            throw new IllegalStateException(
                    "微信支付服务商模式已启用，但 wechat.pay.partner.sp-mch-id 未配置。" +
                    "请通过环境变量 WECHAT_PAY_SP_MCH_ID 注入服务商商户号。");
        }

        if (!StringUtils.hasText(apiV3Key)) {
            throw new IllegalStateException(
                    "微信支付服务商模式已启用，但 wechat.pay.partner.api-v3-key 未配置。" +
                    "请通过环境变量 WECHAT_PAY_API_V3_KEY 注入 API V3 密钥。");
        }

        if (!StringUtils.hasText(certSerialNo)) {
            throw new IllegalStateException(
                    "微信支付服务商模式已启用，但 wechat.pay.partner.cert-serial-no 未配置。" +
                    "请通过环境变量 WECHAT_PAY_CERT_SERIAL_NO 注入商户证书序列号。");
        }

        // 校验私钥：privateKeyPath 和 privateKeyString 至少配置一个
        if (!StringUtils.hasText(privateKeyPath) && !StringUtils.hasText(privateKeyString)) {
            throw new IllegalStateException(
                    "微信支付服务商模式已启用，但 wechat.pay.partner.private-key-path 和 " +
                    "wechat.pay.partner.private-key-string 均未配置。" +
                    "请通过环境变量 WECHAT_PAY_PRIVATE_KEY_PATH 或 WECHAT_PAY_PRIVATE_KEY_STRING 注入商户私钥。");
        }

        log.info("[WeChatPayPartnerProperties] 微信支付服务商模式配置校验通过，spAppId={}***, spMchId={}***",
                maskSensitive(spAppId), maskSensitive(spMchId));
    }

    /**
     * 脱敏敏感信息
     */
    private String maskSensitive(String value) {
        if (value == null || value.length() <= 6) {
            return "***";
        }
        return value.substring(0, 6);
    }
}

