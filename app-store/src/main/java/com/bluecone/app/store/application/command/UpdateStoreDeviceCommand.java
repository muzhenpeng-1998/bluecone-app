package com.bluecone.app.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新门店设备信息的命令。
 * <p>高稳定：仅允许修改必要字段，设备标识需明确。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStoreDeviceCommand {

    private Long tenantId;
    private Long storeId;
    private Long deviceId;
    private String name;
    private String configJson;
    private Long operatorId;
}
