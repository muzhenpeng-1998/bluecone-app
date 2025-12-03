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
}
