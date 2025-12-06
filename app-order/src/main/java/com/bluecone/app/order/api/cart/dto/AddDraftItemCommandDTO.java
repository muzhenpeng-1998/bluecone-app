package com.bluecone.app.order.api.cart.dto;

import lombok.Data;

/**
 * 加购命令。
 */
@Data
public class AddDraftItemCommandDTO {

    private Long productId;

    private Long skuId;

    private Integer quantity;

    /**
     * 规格/口味属性 JSON。
     */
    private String attrsJson;

    private String remark;

    /**
     * 前端传入的单价（分），用于乐观校验。
     */
    private Long clientUnitPrice;
}
