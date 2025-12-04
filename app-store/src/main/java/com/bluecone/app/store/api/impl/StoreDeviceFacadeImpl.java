package com.bluecone.app.store.api.impl;

import com.bluecone.app.store.api.StoreDeviceFacade;
import com.bluecone.app.store.api.dto.StoreDeviceView;
import com.bluecone.app.store.application.command.ChangeStoreDeviceStatusCommand;
import com.bluecone.app.store.application.command.RegisterStoreDeviceCommand;
import com.bluecone.app.store.application.command.UpdateStoreDeviceCommand;
import com.bluecone.app.store.application.query.StoreDeviceListQuery;
import com.bluecone.app.store.application.service.StoreDeviceAppService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门店设备 Facade 实现。
 * <p>职责：简单委派应用服务，保持接口层纯净。</p>
 */
@Service
public class StoreDeviceFacadeImpl implements StoreDeviceFacade {

    private final StoreDeviceAppService appService;

    public StoreDeviceFacadeImpl(StoreDeviceAppService appService) {
        this.appService = appService;
    }

    @Override
    public List<StoreDeviceView> list(StoreDeviceListQuery query) {
        return appService.list(query);
    }

    @Override
    public StoreDeviceView getById(Long tenantId, Long storeId, Long deviceId) {
        return appService.getById(tenantId, storeId, deviceId);
    }

    @Override
    public void registerDevice(RegisterStoreDeviceCommand command) {
        appService.registerDevice(command);
    }

    @Override
    public void updateDevice(UpdateStoreDeviceCommand command) {
        appService.updateDevice(command);
    }

    @Override
    public void changeStatus(ChangeStoreDeviceStatusCommand command) {
        appService.changeStatus(command);
    }
}
