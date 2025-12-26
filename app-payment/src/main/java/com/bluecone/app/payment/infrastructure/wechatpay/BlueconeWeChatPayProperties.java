package com.bluecone.app.payment.infrastructure.wechatpay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信支付 V3 服务商模式配置属性。
 * <p>
 * 服务商密钥信息不进数据库，通过配置文件或环境变量注入。
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "bluecone.wechat.pay")
public class BlueconeWeChatPayProperties {

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
}

