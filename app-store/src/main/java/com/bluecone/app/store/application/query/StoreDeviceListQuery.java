package com.bluecone.app.store.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店设备列表查询对象。
 * <p>支持按类型/状态过滤，便于后台与运行态查询。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreDeviceListQuery {

    private Long tenantId;
    private Long storeId;
    private String deviceType;
    private String status;
}
