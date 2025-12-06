package com.bluecone.app.order.api.cart.dto;

import lombok.Data;

/**
 * 移除购物车明细命令。
 */
@Data
public class RemoveDraftItemCommandDTO {

    private Long skuId;

    private String attrsJson;
}
