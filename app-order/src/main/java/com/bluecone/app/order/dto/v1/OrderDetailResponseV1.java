package com.bluecone.app.dto.order.v1;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单详情响应 - V1 版本
 */
@Data
@Builder
public class OrderDetailResponseV1 {

    /** 订单 ID */
    private Long orderId;

    /** 订单金额 */
    private BigDecimal amount;
}
