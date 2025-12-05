package com.bluecone.app.controller.store;

import com.bluecone.app.api.ApiResponse;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.infra.tenant.TenantContext;
import com.bluecone.app.store.api.StoreDeviceFacade;
import com.bluecone.app.store.api.dto.StoreDeviceView;
import com.bluecone.app.store.application.command.ChangeStoreDeviceStatusCommand;
import com.bluecone.app.store.application.command.RegisterStoreDeviceCommand;
import com.bluecone.app.store.application.command.UpdateStoreDeviceCommand;
import com.bluecone.app.store.application.query.StoreDeviceListQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 门店设备管理后台 Controller。
 * <p>职责：管理打印机/POS/厨房屏等设备。</p>
 */
@RestController
@RequestMapping("/api/admin/store/device")
public class AdminStoreDeviceController {

    private final StoreDeviceFacade storeDeviceFacade;

    public AdminStoreDeviceController(StoreDeviceFacade storeDeviceFacade) {
        this.storeDeviceFacade = storeDeviceFacade;
    }

    /**
     * 查询门店设备列表。
     */
    @GetMapping("/list")
    public ApiResponse<List<StoreDeviceView>> list(@RequestParam Long storeId,
                                                   @RequestParam(required = false) String deviceType,
                                                   @RequestParam(required = false) String status) {
        Long tenantId = requireTenantId();
        StoreDeviceListQuery query = new StoreDeviceListQuery();
        query.setTenantId(tenantId);
        query.setStoreId(storeId);
        query.setDeviceType(deviceType);
        query.setStatus(status);
        return ApiResponse.success(storeDeviceFacade.list(query));
    }

    /**
     * 查看单个设备详情。
     */
    @GetMapping("/detail")
    public ApiResponse<StoreDeviceView> detail(@RequestParam Long storeId,
                                               @RequestParam Long deviceId) {
        Long tenantId = requireTenantId();
        return ApiResponse.success(storeDeviceFacade.getById(tenantId, storeId, deviceId));
    }

    /**
     * 注册新设备。
     */
    @PostMapping
    public ApiResponse<Void> register(@RequestBody RegisterStoreDeviceCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeDeviceFacade.registerDevice(command);
        return ApiResponse.success();
    }

    /**
     * 更新设备配置。
     */
    @PutMapping
    public ApiResponse<Void> update(@RequestBody UpdateStoreDeviceCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeDeviceFacade.updateDevice(command);
        return ApiResponse.success();
    }

    /**
     * 修改设备状态（启用/停用）。
     */
    @PutMapping("/status")
    public ApiResponse<Void> changeStatus(@RequestBody ChangeStoreDeviceStatusCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeDeviceFacade.changeStatus(command);
        return ApiResponse.success();
    }

    private Long requireTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null || tenantIdStr.isBlank()) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED, "租户未登录或上下文缺失");
        }
        try {
            return Long.parseLong(tenantIdStr);
        } catch (NumberFormatException ex) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "非法的租户标识");
        }
    }
}
