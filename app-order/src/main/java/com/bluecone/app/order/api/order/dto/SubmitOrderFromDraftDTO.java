package com.bluecone.app.order.api.order.dto;

import lombok.Data;

/**
 * 草稿订单提交请求 DTO。
 */
@Data
public class SubmitOrderFromDraftDTO {

    private Long tenantId;

    private Long storeId;

    private Long userId;

    private String channel;

    private String scene;

    private String orderToken;

    private Long clientPayableAmount;

    private String userRemark;

    private String contactName;

    private String contactPhone;

    private String addressJson;
}
