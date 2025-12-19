package com.bluecone.app.order.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商户拒单接口请求参数。
 */
@Data
public class MerchantRejectOrderRequest {

    @NotNull(message = "tenantId 不能为空")
    private Long tenantId;

    @NotNull(message = "storeId 不能为空")
    private Long storeId;

    @NotNull(message = "operatorId 不能为空")
    private Long operatorId;

    @NotBlank(message = "reasonCode 不能为空")
    private String reasonCode;

    private String reasonDesc;

    /**
     * 请求ID（用于幂等，建议使用 UUID）。
     */
    @NotBlank(message = "requestId 不能为空")
    private String requestId;

    /**
     * 期望的订单版本号（用于乐观锁，可选）。
     * <p>如果不传，则不校验版本号（不推荐）。</p>
     */
    private Integer expectedVersion;
}
