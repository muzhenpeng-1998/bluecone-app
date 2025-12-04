package com.bluecone.app.store.application.service;

import com.bluecone.app.store.api.dto.StoreDeviceView;
import com.bluecone.app.store.dao.entity.BcStoreDevice;
import org.springframework.stereotype.Component;

/**
 * 设备装配器：实体 → 对外视图 DTO。
 * <p>高隔离：屏蔽底层实体字段变化。</p>
 */
@Component
public class StoreDeviceAssembler {

    public StoreDeviceView toView(BcStoreDevice entity) {
        if (entity == null) {
            return null;
        }
        return StoreDeviceView.builder()
                .tenantId(entity.getTenantId())
                .storeId(entity.getStoreId())
                .deviceId(entity.getId())
                .deviceType(entity.getDeviceType())
                .name(entity.getName())
                .sn(entity.getSn())
                .status(entity.getStatus())
                .configJson(entity.getConfigJson())
                .configSummary(null) // TODO: 解析配置摘要
                .build();
    }
}
