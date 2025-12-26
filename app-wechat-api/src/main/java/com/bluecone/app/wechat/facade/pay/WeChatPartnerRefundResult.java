package com.bluecone.app.wechat.facade.pay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 微信支付服务商退款结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeChatPartnerRefundResult {

    /**
     * 微信退款单号（refund_id）。
     */
    private String refundId;

    /**
     * 商户退款单号（out_refund_no）。
     */
    private String outRefundNo;

    /**
     * 微信支付单号（transaction_id）。
     */
    private String transactionId;

    /**
     * 商户订单号（out_trade_no）。
     */
    private String outTradeNo;

    /**
     * 退款渠道（ORIGINAL / BALANCE / OTHER_BALANCE / OTHER_BANKCARD）。
     */
    private String channel;

    /**
     * 退款入账账户。
     */
    private String userReceivedAccount;

    /**
     * 退款成功时间。
     */
    private Instant successTime;

    /**
     * 退款创建时间。
     */
    private Instant createTime;

    /**
     * 退款状态（SUCCESS / CLOSED / PROCESSING / ABNORMAL）。
     */
    private String status;

    /**
     * 退款金额（单位：分）。
     */
    private Long refundAmount;

    /**
     * 用户实际退款金额（单位：分）。
     */
    private Long payerRefundAmount;
}

