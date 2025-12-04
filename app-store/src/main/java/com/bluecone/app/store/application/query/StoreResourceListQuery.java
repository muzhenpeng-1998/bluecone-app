package com.bluecone.app.store.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店资源列表查询对象。
 * <p>高稳定：支持按类型/分区/状态过滤。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreResourceListQuery {

    private Long tenantId;
    private Long storeId;
    private String resourceType;
    private String area;
    private String status;
}
