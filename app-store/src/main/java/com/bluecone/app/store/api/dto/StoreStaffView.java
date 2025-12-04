package com.bluecone.app.store.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店员工关系视图，描述门店与平台用户的绑定关系。
 * <p>高隔离：只读 DTO，外部不直接操作实体与 Mapper。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreStaffView {

    private Long tenantId;
    private Long storeId;
    private Long staffId;
    private Long userId;
    private String role;
    private Long createdAt;
}
