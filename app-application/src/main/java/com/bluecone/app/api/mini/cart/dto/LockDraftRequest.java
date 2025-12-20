package com.bluecone.app.api.mini.cart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 锁定购物车请求。
 */
@Data
public class LockDraftRequest {

    @NotNull
    private Long storeId;

    /**
     * 客户端金额（分）用于校验。
     */
    private Long clientPayableAmount;

    /**
     * 前端幂等 token。
     */
    @NotBlank
    private String orderToken;
}
