package com.bluecone.app.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单提交单响应（M0）。
 * <p>包含订单ID、对外展示的订单号、状态等信息。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSubmitResponse {

    /**
     * 订单ID（内部主键，ULID）。
     */
    private Long orderId;

    /**
     * 对外展示的订单号（PublicId格式：ord_xxx）。
     */
    private String publicOrderNo;

    /**
     * 订单状态（WAIT_PAY：待支付）。
     */
    private String status;

    /**
     * 应付金额（实际支付金额）。
     */
    private BigDecimal payableAmount;

    /**
     * 币种（默认CNY）。
     */
    private String currency;

    /**
     * 是否为幂等返回（true=重复提交，false=首次创建）。
     */
    private Boolean idempotent;
}
