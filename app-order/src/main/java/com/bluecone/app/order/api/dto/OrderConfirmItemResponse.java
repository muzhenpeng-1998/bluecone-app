package com.bluecone.app.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 订单确认单明细项响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmItemResponse {

    /**
     * SKU ID。
     */
    private Long skuId;

    /**
     * 商品ID。
     */
    private Long productId;

    /**
     * 商品名称（快照）。
     */
    private String productName;

    /**
     * SKU名称（快照）。
     */
    private String skuName;

    /**
     * 商品编码（快照）。
     */
    private String productCode;

    /**
     * 购买数量。
     */
    private Integer quantity;

    /**
     * 单价（快照）。
     */
    private BigDecimal unitPrice;

    /**
     * 优惠金额（M0暂时为0）。
     */
    private BigDecimal discountAmount;

    /**
     * 应付金额（单价*数量-优惠）。
     */
    private BigDecimal payableAmount;

    /**
     * 商品属性（规格、口味等）。
     */
    private Map<String, Object> attrs;

    /**
     * 明细备注。
     */
    private String remark;
}
