package com.bluecone.app.product.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SKU 上架命令，面向应用服务的语义化入参。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishSkuCommand {

    private Long tenantId;

    /**
     * 门店维度的上架，可为空表示全局。
     */
    private Long storeId;

    /**
     * 所属 SPU ID。
     */
    private Long productId;

    /**
     * 具体 SKU ID。
     */
    private Long skuId;

    /**
     * 操作人，用于审计。
     */
    private Long operatorId;
}
