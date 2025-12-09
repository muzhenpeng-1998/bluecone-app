package com.bluecone.app.product.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SKU 改价命令，金额使用“分”为单位，避免精度问题。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeSkuPriceCommand {

    private Long tenantId;

    private Long storeId;

    private Long productId;

    private Long skuId;

    /**
     * 新售价（单位：分）。
     */
    private Long newPrice;

    private Long operatorId;
}
