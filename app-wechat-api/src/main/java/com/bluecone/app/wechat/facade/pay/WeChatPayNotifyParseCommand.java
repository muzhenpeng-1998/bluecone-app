package com.bluecone.app.wechat.facade.pay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信支付回调解析命令。
 * <p>
 * 包含原始报文和签名头信息。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatPayNotifyParseCommand {

    /**
     * 原始请求体（加密报文）。
     */
    private String rawBody;

    /**
     * 微信签名时间戳（HTTP 头：Wechatpay-Timestamp）。
     */
    private String timestamp;

    /**
     * 微信签名随机串（HTTP 头：Wechatpay-Nonce）。
     */
    private String nonce;

    /**
     * 微信签名值（HTTP 头：Wechatpay-Signature）。
     */
    private String signature;

    /**
     * 微信平台证书序列号（HTTP 头：Wechatpay-Serial）。
     */
    private String serial;
}

