package com.bluecone.app.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 移除门店员工关系的命令。
 * <p>高稳定：明确租户/门店/用户维度，便于审计。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveStoreStaffCommand {

    private Long tenantId;
    private Long storeId;
    private Long userId;
    private Long operatorId;
}
