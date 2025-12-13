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
    /**
     * 内部门店 ID（仅内部使用）。
     */
    private Long storeId;
    /**
     * 对外门店 ID（PublicId）。
     */
    private String storePublicId;
    /**
     * 支持按门店编码查询。
     */
    private String storeCode;
}
