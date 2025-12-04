package com.bluecone.app.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注册门店设备的命令（打印机/POS/厨房屏等）。
 * <p>高并发：设备信息后续会参与打印路由、厨房展示。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterStoreDeviceCommand {

    private Long tenantId;
    private Long storeId;
    private String deviceType;
    private String name;
    private String sn;
    private String configJson;
    private Long operatorId;
}
