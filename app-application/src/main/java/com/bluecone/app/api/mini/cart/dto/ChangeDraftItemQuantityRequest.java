package com.bluecone.app.api.mini.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改购物车数量请求。
 */
@Data
public class ChangeDraftItemQuantityRequest {

    @NotNull
    private Long storeId;

    @NotNull
    private String attrsJson;

    @NotNull
    @Min(1)
    private Integer newQuantity;
}
