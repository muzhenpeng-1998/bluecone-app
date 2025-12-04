package com.bluecone.app.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 调整门店设备状态（启用/停用等）的命令。
 * <p>高隔离：应用层契约，后续可扩展幂等/审计。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeStoreDeviceStatusCommand {

    private Long tenantId;
    private Long storeId;
    private Long deviceId;
    private String targetStatus;
    private Long operatorId;
}
