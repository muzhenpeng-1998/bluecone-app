package com.bluecone.app.store.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店设备视图（打印机/POS/厨房屏等），只读 DTO。
 * <p>高隔离：对外仅暴露设备基础信息与配置摘要。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreDeviceView {

    private Long tenantId;
    private Long storeId;
    private Long deviceId;
    private String deviceType;
    private String name;
    private String sn;
    private String status;
    private String configJson;
    private String configSummary;
}
