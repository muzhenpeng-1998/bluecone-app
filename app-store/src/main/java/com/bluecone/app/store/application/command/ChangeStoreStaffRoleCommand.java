package com.bluecone.app.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 调整门店员工角色的命令。
 * <p>高隔离：只描述意图，领域内处理授权与一致性。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeStoreStaffRoleCommand {

    private Long tenantId;
    private Long storeId;
    private Long userId;
    private String newRole;
    private Long operatorId;
}
