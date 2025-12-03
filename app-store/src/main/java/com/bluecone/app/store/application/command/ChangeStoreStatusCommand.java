package com.bluecone.app.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 切换门店状态（OPEN/PAUSED/CLOSED）的写侧命令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeStoreStatusCommand {
    private Long tenantId;
    private Long storeId;
    private String status;
    private Long expectedConfigVersion;
}
