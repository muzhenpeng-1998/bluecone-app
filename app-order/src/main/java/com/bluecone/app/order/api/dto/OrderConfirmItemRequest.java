package com.bluecone.app.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 订单确认单明细项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmItemRequest {

    /**
     * SKU ID（必填）。
     */
    private Long skuId;

    /**
     * 商品ID（可选，用于快照）。
     */
    private Long productId;

    /**
     * 购买数量（必填，大于0）。
     */
    private Integer quantity;

    /**
     * 客户端传递的单价（可选，用于金额校验）。
     */
    private BigDecimal clientUnitPrice;

    /**
     * 商品属性（可选，如规格、口味等）。
     */
    private Map<String, Object> attrs;

    /**
     * 明细备注（可选）。
     */
    private String remark;
}
