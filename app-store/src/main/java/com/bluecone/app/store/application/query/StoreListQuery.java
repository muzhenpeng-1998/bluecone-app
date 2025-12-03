package com.bluecone.app.store.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店列表查询对象，供应用层编排查询用例。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreListQuery {
    private Long tenantId;
    private String cityCode;
    private String status;
    private String keyword;
}
