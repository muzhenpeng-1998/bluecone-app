package com.bluecone.app.order.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商户出餐完成订单接口请求参数。
 */
@Data
public class MerchantMarkReadyRequest {

    @NotNull(message = "tenantId 不能为空")
    private Long tenantId;

    @NotNull(message = "storeId 不能为空")
    private Long storeId;

    @NotNull(message = "operatorId 不能为空")
    private Long operatorId;

    /**
     * 请求ID（用于幂等，建议使用 UUID）。
     */
    @jakarta.validation.constraints.NotBlank(message = "requestId 不能为空")
    private String requestId;

    /**
     * 期望的订单版本号（用于乐观锁，可选）。
     * <p>如果不传，则不校验版本号（不推荐）。</p>
     */
    private Integer expectedVersion;
}
