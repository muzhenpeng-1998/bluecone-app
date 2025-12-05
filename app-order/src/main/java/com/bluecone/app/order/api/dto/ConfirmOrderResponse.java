package com.bluecone.app.order.api.dto;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 确认订单接口的响应模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmOrderResponse {

    /**
     * 订单ID（主键）。
     */
    private Long orderId;

    /**
     * 对外展示的订单号。
     */
    private String orderNo;

    /**
     * 订单主状态：PENDING_PAYMENT / PAID / CANCELLED / COMPLETED 等。
     */
    private String status;

    /**
     * 支付状态：UNPAID / PAID / PARTIAL_REFUND / FULL_REFUND 等。
     */
    private String payStatus;

    /**
     * 订单最终应付金额（服务端计算结果）。
     */
    @Builder.Default
    private BigDecimal payableAmount = BigDecimal.ZERO;

    /**
     * 币种，默认 CNY。
     */
    @Builder.Default
    private String currency = "CNY";

    /**
     * 实际使用的支付渠道（可能和请求的 payChannel 一致）。
     */
    private String payChannel;

    /**
     * 是否需要支付。
     */
    @Builder.Default
    private Boolean needPay = Boolean.TRUE;

    /**
     * 支付超时时间（秒），超过该时间未支付可自动取消订单。
     */
    @Builder.Default
    private Integer paymentTimeoutSeconds = 0;

    /**
     * 预留的支付参数占位，例如微信 JSAPI 的 prepayId。
     */
    @Builder.Default
    private Map<String, Object> paymentPayload = Collections.emptyMap();

    /**
     * 其他扩展字段。
     */
    @Builder.Default
    private Map<String, Object> ext = Collections.emptyMap();
}
