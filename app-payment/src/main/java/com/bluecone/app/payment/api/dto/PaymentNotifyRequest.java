package com.bluecone.app.payment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付回调请求（M1）。
 * <p>用于接收支付渠道的回调通知或模拟回调。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentNotifyRequest {

    /**
     * 通知ID（必填，用于幂等）。
     * <p>真实场景来自渠道回调，M1 可由客户端生成UUID。</p>
     */
    private String notifyId;

    /**
     * 渠道支付单号（必填）。
     * <p>如微信的 transaction_id、支付宝的 trade_no。</p>
     */
    private String payNo;

    /**
     * 订单ID（必填）。
     */
    private Long orderId;

    /**
     * 实际支付金额（必填）。
     */
    private BigDecimal amount;

    /**
     * 支付完成时间（可选，默认当前时间）。
     */
    private LocalDateTime paidAt;

    /**
     * 支付渠道（可选）。
     */
    private String channel;

    /**
     * 租户ID（可选，用于多租户场景）。
     */
    private Long tenantId;
}
