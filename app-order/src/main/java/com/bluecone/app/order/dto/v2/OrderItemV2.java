package com.bluecone.app.dto.order.v2;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细 - V2 版本
 */
@Data
@Builder
public class OrderItemV2 {

    /** 明细 ID */
    private Long itemId;

    /** 商品名称 */
    private String productName;

    /** 数量 */
    private Integer quantity;

    /** 单价 */
    private BigDecimal price;
}
