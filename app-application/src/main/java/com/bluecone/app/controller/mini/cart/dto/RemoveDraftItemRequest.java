package com.bluecone.app.controller.mini.cart.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 移除购物车行请求。
 */
@Data
public class RemoveDraftItemRequest {

    @NotNull
    private Long storeId;

    private String attrsJson;
}
