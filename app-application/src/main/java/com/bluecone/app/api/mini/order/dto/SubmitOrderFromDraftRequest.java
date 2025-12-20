package com.bluecone.app.api.mini.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 草稿订单提交请求（微信小程序）。
 */
@Data
public class SubmitOrderFromDraftRequest {

    @NotNull
    private Long storeId;

    /**
     * 前端金额（分）。
     */
    @NotNull
    @Min(0)
    private Long clientPayableAmount;

    /**
     * 幂等 token。
     */
    @NotBlank
    private String orderToken;

    private String userRemark;

    private String contactName;

    private String contactPhone;

    private String addressJson;

    private String scene;
}
