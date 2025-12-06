package com.bluecone.app.order.api.cart.dto;

import java.util.List;
import lombok.Data;

/**
 * 订单草稿视图。
 */
@Data
public class OrderDraftViewDTO {

    private Long orderId;

    private Long tenantId;

    private Long storeId;

    private Long userId;

    private String channel;

    private String scene;

    /**
     * 草稿状态，如 DRAFT/LOCKED。
     */
    private String state;

    /**
     * 原始总金额（分）。
     */
    private Long originTotalAmount;

    /**
     * 折扣总金额（分）。
     */
    private Long discountTotalAmount;

    /**
     * 应付总金额（分）。
     */
    private Long payableTotalAmount;

    private List<OrderDraftItemDTO> items;

    private String extJson;

    private Long version;
}
