package com.bluecone.app.payment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 支付创建响应（M1）。
 * <p>包含支付单ID、预支付信息等。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateResponse {

    /**
     * 支付单ID（内部主键）。
     */
    private Long paymentId;

    /**
     * 支付单号（对外展示）。
     */
    private String paymentNo;

    /**
     * 订单ID。
     */
    private Long orderId;

    /**
     * 订单号。
     */
    private String orderNo;

    /**
     * 应付金额。
     */
    private BigDecimal payableAmount;

    /**
     * 币种。
     */
    private String currency;

    /**
     * 支付渠道。
     */
    private String channel;

    /**
     * Mock 支付令牌（M1 简化实现，用于模拟支付）。
     * <p>真实场景应返回微信 prepay_id、支付宝 trade_no 等。</p>
     */
    private String mockPayToken;

    /**
     * 是否为复用的支付单（幂等返回）。
     */
    private Boolean reused;
}
