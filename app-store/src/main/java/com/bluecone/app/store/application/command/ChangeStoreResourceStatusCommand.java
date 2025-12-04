package com.bluecone.app.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 调整门店资源状态的命令（启用/禁用/移除等）。
 * <p>高隔离：仅承担输入契约，实际状态流转由应用/领域处理。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeStoreResourceStatusCommand {

    private Long tenantId;
    private Long storeId;
    private Long resourceId;
    private String targetStatus;
    private Long operatorId;
}
