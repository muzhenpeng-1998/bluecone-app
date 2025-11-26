package com.bluecone.app.dto.order.v2;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单详情响应 - V2 版本
 */
@Data
@Builder
public class OrderDetailResponseV2 {

    /** 订单 ID */
    private Long orderId;

    /** 订单金额 */
    private BigDecimal amount;

    /** 订单状态（CREATED/PAID/CANCELED） */
    private String status;

    /** 订单明细列表 */
    private List<OrderItemV2> items;
}
