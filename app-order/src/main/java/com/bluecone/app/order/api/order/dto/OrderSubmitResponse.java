package com.bluecone.app.order.api.order.dto;

import lombok.Data;

/**
 * 提交订单后返回的基础数据。
 */
@Data
public class OrderSubmitResponse {

    private Long orderId;

    private String orderNo;

    private String status;

    private String payStatus;

    private Long payableAmount;

    private String currency;

    private String channel;

    private String scene;
}
