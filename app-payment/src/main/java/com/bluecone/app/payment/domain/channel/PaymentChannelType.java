package com.bluecone.app.payment.domain.channel;

import java.util.Arrays;

/**
 * 支付渠道类型枚举。
 */
public enum PaymentChannelType {

    WECHAT_JSAPI("WECHAT_JSAPI", "微信小程序/公众号 JSAPI 支付"),
    WECHAT_NATIVE("WECHAT_NATIVE", "微信扫码支付"),
    ALIPAY_MINI("ALIPAY_MINI", "支付宝小程序支付");

    private final String code;
    private final String desc;

    PaymentChannelType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据编码解析渠道类型，忽略大小写，未匹配返回 null。
     */
    public static PaymentChannelType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 从支付渠道与支付方式组合推导渠道类型。
     *
     * @param channel 支付渠道
     * @param method  支付方式
     */
    public static PaymentChannelType fromChannelAndMethod(com.bluecone.app.payment.domain.enums.PaymentChannel channel,
                                                          com.bluecone.app.payment.domain.enums.PaymentMethod method) {
        if (channel == null || method == null) {
            return null;
        }
        String methodCode = method.getCode();
        String channelCode = channel.getCode();
        String key = channelCode + "_" + methodCode;
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(key) || item.code.equalsIgnoreCase(methodCode))
                .findFirst()
                .orElse(null);
    }
}
