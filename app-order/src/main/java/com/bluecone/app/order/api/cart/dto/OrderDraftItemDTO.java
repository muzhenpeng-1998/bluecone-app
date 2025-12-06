package com.bluecone.app.order.api.cart.dto;

import lombok.Data;

/**
 * 订单草稿明细视图。
 */
@Data
public class OrderDraftItemDTO {

    private Long productId;

    private Long skuId;

    private String productName;

    private String skuName;

    private String productPictureUrl;

    private Integer quantity;

    /**
     * 规格/口味等属性 JSON。
     */
    private String attrsJson;

    private String remark;

    /**
     * 原始单价（分）。
     */
    private Long originUnitPrice;

    /**
     * 实际单价（分）。
     */
    private Long unitPrice;

    /**
     * 折扣金额（分）。
     */
    private Long discountAmount;

    /**
     * 应付金额（分）。
     */
    private Long payableAmount;
}
