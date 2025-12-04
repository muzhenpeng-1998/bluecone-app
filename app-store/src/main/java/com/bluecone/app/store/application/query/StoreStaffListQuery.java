package com.bluecone.app.store.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店员工列表查询对象。
 * <p>支持按用户/角色过滤，预留批量查询能力。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreStaffListQuery {

    private Long tenantId;
    private Long storeId;
    private Long userId;
    private String role;
}
