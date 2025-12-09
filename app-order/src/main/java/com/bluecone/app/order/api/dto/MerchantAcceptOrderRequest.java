package com.bluecone.app.order.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商户接单接口请求参数，当前由前端传入 tenant/store/operator。
 */
@Data
public class MerchantAcceptOrderRequest {

    @NotNull(message = "tenantId 不能为空")
    private Long tenantId;

    @NotNull(message = "storeId 不能为空")
    private Long storeId;

    @NotNull(message = "operatorId 不能为空")
    private Long operatorId;
}
