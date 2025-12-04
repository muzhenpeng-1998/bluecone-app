package com.bluecone.app.store.api;

import com.bluecone.app.store.api.dto.StoreDeviceView;
import com.bluecone.app.store.application.command.ChangeStoreDeviceStatusCommand;
import com.bluecone.app.store.application.command.RegisterStoreDeviceCommand;
import com.bluecone.app.store.application.command.UpdateStoreDeviceCommand;
import com.bluecone.app.store.application.query.StoreDeviceListQuery;

import java.util.List;

/**
 * 门店设备管理 Facade（打印机/POS/厨房屏等）。
 * <p>高隔离：对外统一设备读写接口，屏蔽底层实现。</p>
 */
public interface StoreDeviceFacade {

    List<StoreDeviceView> list(StoreDeviceListQuery query);

    StoreDeviceView getById(Long tenantId, Long storeId, Long deviceId);

    void registerDevice(RegisterStoreDeviceCommand command);

    void updateDevice(UpdateStoreDeviceCommand command);

    void changeStatus(ChangeStoreDeviceStatusCommand command);
}
