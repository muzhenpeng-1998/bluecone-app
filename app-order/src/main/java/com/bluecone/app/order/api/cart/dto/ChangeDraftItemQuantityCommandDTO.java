package com.bluecone.app.order.api.cart.dto;

import lombok.Data;

/**
 * 修改购物车明细数量命令。
 */
@Data
public class ChangeDraftItemQuantityCommandDTO {

    private Long skuId;

    private String attrsJson;

    private Integer newQuantity;
}
