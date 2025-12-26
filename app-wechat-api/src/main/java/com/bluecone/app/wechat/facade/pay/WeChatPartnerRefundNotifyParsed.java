package com.bluecone.app.wechat.facade.pay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 微信支付服务商模式退款回调解析结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatPartnerRefundNotifyParsed {

    /**
     * 原始报文（用于日志或审计）。
     */
    private String rawBody;

    /**
     * 服务商商户号（sp_mchid）。
     */
    private String spMchId;

    /**
     * 子商户号（sub_mchid）。
     */
    private String subMchId;

    /**
     * 商户订单号（out_trade_no）。
     */
    private String outTradeNo;

    /**
     * 微信支付单号（transaction_id）。
     */
    private String transactionId;

    /**
     * 商户退款单号（out_refund_no）。
     */
    private String outRefundNo;

    /**
     * 微信退款单号（refund_id）。
     */
    private String refundId;

    /**
     * 退款状态（SUCCESS / CLOSED / ABNORMAL）。
     */
    private String refundStatus;

    /**
     * 退款成功时间。
     */
    private Instant successTime;

    /**
     * 退款入账账户。
     */
    private String userReceivedAccount;

    /**
     * 退款金额（单位：分）。
     */
    private Long refundAmount;

    /**
     * 订单总金额（单位：分）。
     */
    private Long totalAmount;
}

