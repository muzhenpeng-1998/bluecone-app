package com.bluecone.app.order.api.cart.dto;

import lombok.Data;

/**
 * 锁定购物车命令，预留下单前校验字段。
 */
@Data
public class LockDraftCommandDTO {

    /**
     * 客户端计算的应付金额（分），用于前后端乐观校验。
     */
    private Long clientPayableAmount;

    /**
     * 防重复的客户端 token，后续串下单可用。
     */
    private String orderToken;
}
