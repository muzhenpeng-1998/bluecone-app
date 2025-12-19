package com.bluecone.app.store.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.store.api.dto.StoreDeviceView;
import com.bluecone.app.store.application.command.ChangeStoreDeviceStatusCommand;
import com.bluecone.app.store.application.command.RegisterStoreDeviceCommand;
import com.bluecone.app.store.application.command.UpdateStoreDeviceCommand;
import com.bluecone.app.store.application.query.StoreDeviceListQuery;
import com.bluecone.app.store.dao.entity.BcStoreDevice;
import com.bluecone.app.store.domain.error.StoreErrorCode;
import com.bluecone.app.store.dao.service.IBcStoreDeviceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 门店设备应用服务（打印机/POS/厨房屏等）。
 * <p>职责：处理设备读写，保障多租户隔离与逻辑删除。</p>
 * <p>扩展点：可接入设备注册校验、幂等、事件推送等。</p>
 */
@Service
public class StoreDeviceAppService {

    private final IBcStoreDeviceService bcStoreDeviceService;
    private final StoreDeviceAssembler storeDeviceAssembler;

    public StoreDeviceAppService(IBcStoreDeviceService bcStoreDeviceService,
                                 StoreDeviceAssembler storeDeviceAssembler) {
        this.bcStoreDeviceService = bcStoreDeviceService;
        this.storeDeviceAssembler = storeDeviceAssembler;
    }

    public List<StoreDeviceView> list(StoreDeviceListQuery query) {
        LambdaQueryWrapper<BcStoreDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BcStoreDevice::getTenantId, query.getTenantId())
                .eq(BcStoreDevice::getStoreId, query.getStoreId())
                .eq(BcStoreDevice::getIsDeleted, false);
        if (query.getDeviceType() != null) {
            wrapper.eq(BcStoreDevice::getDeviceType, query.getDeviceType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(BcStoreDevice::getStatus, query.getStatus());
        }
        return bcStoreDeviceService.list(wrapper).stream()
                .map(storeDeviceAssembler::toView)
                .collect(Collectors.toList());
    }

    public StoreDeviceView getById(Long tenantId, Long storeId, Long deviceId) {
        BcStoreDevice entity = bcStoreDeviceService.lambdaQuery()
                .eq(BcStoreDevice::getTenantId, tenantId)
                .eq(BcStoreDevice::getStoreId, storeId)
                .eq(BcStoreDevice::getId, deviceId)
                .eq(BcStoreDevice::getIsDeleted, false)
                .one();
        if (entity == null) {
            throw new BusinessException(StoreErrorCode.DEVICE_NOT_FOUND);
        }
        return storeDeviceAssembler.toView(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void registerDevice(RegisterStoreDeviceCommand command) {
        BcStoreDevice entity = new BcStoreDevice();
        entity.setTenantId(command.getTenantId());
        entity.setStoreId(command.getStoreId());
        entity.setDeviceType(command.getDeviceType());
        entity.setName(command.getName());
        entity.setSn(command.getSn());
        entity.setConfigJson(command.getConfigJson());
        entity.setStatus("ENABLED");
        entity.setIsDeleted(false);
        bcStoreDeviceService.save(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDevice(UpdateStoreDeviceCommand command) {
        boolean updated = bcStoreDeviceService.lambdaUpdate()
                .eq(BcStoreDevice::getTenantId, command.getTenantId())
                .eq(BcStoreDevice::getStoreId, command.getStoreId())
                .eq(BcStoreDevice::getId, command.getDeviceId())
                .eq(BcStoreDevice::getIsDeleted, false)
                .set(command.getName() != null, BcStoreDevice::getName, command.getName())
                .set(command.getConfigJson() != null, BcStoreDevice::getConfigJson, command.getConfigJson())
                .update();
        if (!updated) {
            throw new BusinessException(StoreErrorCode.DEVICE_NOT_FOUND, "更新设备信息失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(ChangeStoreDeviceStatusCommand command) {
        boolean updated = bcStoreDeviceService.lambdaUpdate()
                .eq(BcStoreDevice::getTenantId, command.getTenantId())
                .eq(BcStoreDevice::getStoreId, command.getStoreId())
                .eq(BcStoreDevice::getId, command.getDeviceId())
                .eq(BcStoreDevice::getIsDeleted, false)
                .set(BcStoreDevice::getStatus, command.getTargetStatus())
                .update();
        if (!updated) {
            throw new BusinessException(StoreErrorCode.DEVICE_NOT_FOUND, "更新设备状态失败");
        }
    }
}
