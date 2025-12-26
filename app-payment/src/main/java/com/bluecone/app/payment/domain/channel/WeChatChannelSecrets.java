package com.bluecone.app.payment.domain.channel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 微信支付渠道的敏感配置。
 * <p>
 * 此处仅承载密文/加密字段的载体，不处理具体解密逻辑。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatChannelSecrets {

    /** 商户号 */
    private String mchId;

    /** 应用 AppId */
    private String appId;

    /** 子商户号，可为空 */
    private String subMchId;

    /** APIv3 Key 的密文 */
    private String encApiV3Key;

    /** 证书序列号密文或明文 */
    private String encSerialNo;

    /**
     * 渠道模式：SERVICE_PROVIDER（服务商）或 DIRECT（直连）。
     * <p>
     * 默认为 SERVICE_PROVIDER，用于标识当前配置是服务商模式还是直连模式。
     * 未来如需同时支持两种模式，可根据此字段路由到不同的下单逻辑。
     * </p>
     */
    @Builder.Default
    private String channelMode = "SERVICE_PROVIDER";
}
