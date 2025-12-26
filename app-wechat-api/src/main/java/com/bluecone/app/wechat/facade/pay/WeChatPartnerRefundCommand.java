package com.bluecone.app.wechat.facade.pay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信支付服务商退款命令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatPartnerRefundCommand {

    /**
     * 子商户号（sub_mchid）。
     */
    private String subMchId;

    /**
     * 商户订单号（out_trade_no，与 transactionId 二选一）。
     */
    private String outTradeNo;

    /**
     * 微信支付单号（transaction_id，与 outTradeNo 二选一）。
     */
    private String transactionId;

    /**
     * 商户退款单号（out_refund_no）。
     */
    private String outRefundNo;

    /**
     * 退款原因（可选）。
     */
    private String reason;

    /**
     * 退款金额（单位：分）。
     */
    private Long refundAmount;

    /**
     * 原订单金额（单位：分）。
     */
    private Long totalAmount;

    /**
     * 货币类型（默认 CNY）。
     */
    private String currency;

    /**
     * 退款回调地址（可选）。
     */
    private String notifyUrl;
}

