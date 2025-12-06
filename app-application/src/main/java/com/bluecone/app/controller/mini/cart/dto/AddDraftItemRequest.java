package com.bluecone.app.controller.mini.cart.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 小程序加购请求。
 */
@Data
public class AddDraftItemRequest {

    @NotNull
    private Long storeId;

    @NotNull
    private Long productId;

    @NotNull
    private Long skuId;

    @NotNull
    @Min(1)
    private Integer quantity;

    private String attrsJson;

    private String remark;

    private Long clientUnitPrice;

    /**
     * 客户端生成的幂等键。
     */
    @NotBlank
    private String clientRequestId;

    @JsonIgnore
    private Long tenantId;

    @JsonIgnore
    private Long userId;
}
