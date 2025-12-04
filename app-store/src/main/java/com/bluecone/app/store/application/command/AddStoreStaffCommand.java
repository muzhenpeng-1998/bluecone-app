package com.bluecone.app.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新增门店员工的命令。
 * <p>多租户：需明确 tenantId/storeId；高隔离：仅作入参承载。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddStoreStaffCommand {

    private Long tenantId;
    private Long storeId;
    private Long userId;
    private String role;
    private Long operatorId;
}
