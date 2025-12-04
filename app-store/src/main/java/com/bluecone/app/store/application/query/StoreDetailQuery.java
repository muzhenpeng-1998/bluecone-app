package com.bluecone.app.store.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店详情查询对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreDetailQuery {
    private Long tenantId;
    private Long storeId;
    /**
     * 支持按门店编码查询。
     */
    private String storeCode;
}
