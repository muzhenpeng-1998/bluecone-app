package com.bluecone.app.application.controller.store;

import com.bluecone.app.api.ApiResponse;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.infra.tenant.TenantContext;
import com.bluecone.app.store.api.StoreFacade;
import com.bluecone.app.store.api.dto.StoreBaseView;
import com.bluecone.app.store.application.command.ChangeStoreStatusCommand;
import com.bluecone.app.store.application.command.CreateStoreCommand;
import com.bluecone.app.store.application.command.ToggleOpenForOrdersCommand;
import com.bluecone.app.store.application.command.UpdateStoreBaseCommand;
import com.bluecone.app.store.application.command.UpdateStoreCapabilitiesCommand;
import com.bluecone.app.store.application.command.UpdateStoreOpeningHoursCommand;
import com.bluecone.app.store.application.command.UpdateStoreSpecialDaysCommand;
import com.bluecone.app.store.application.query.StoreDetailQuery;
import com.bluecone.app.store.application.query.StoreListQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 门店管理后台 Controller。
 * <p>职责：为运营/租户后台提供门店的增删改查、能力配置、营业时间配置等管理接口。</p>
 * <p>高隔离：仅依赖 StoreFacade，不直接访问 Mapper/ServiceImpl。</p>
 */
@RestController
@RequestMapping("/api/admin/store")
public class AdminStoreController {

    private final StoreFacade storeFacade;

    public AdminStoreController(StoreFacade storeFacade) {
        this.storeFacade = storeFacade;
    }

    /**
     * 列表查询门店。
     */
    @GetMapping("/list")
    public ApiResponse<List<StoreBaseView>> list(StoreListQuery query) {
        Long tenantId = requireTenantId();
        query.setTenantId(tenantId);
        List<StoreBaseView> list = storeFacade.list(query);
        return ApiResponse.success(list);
    }

    /**
     * 门店详情（基础信息）。
     */
    @GetMapping("/detail")
    public ApiResponse<StoreBaseView> detail(@RequestParam(required = false) Long storeId,
                                             @RequestParam(required = false) String storeCode) {
        Long tenantId = requireTenantId();
        StoreDetailQuery query = new StoreDetailQuery();
        query.setTenantId(tenantId);
        query.setStoreId(storeId);
        query.setStoreCode(storeCode);
        StoreBaseView view = storeFacade.detail(query);
        return ApiResponse.success(view);
    }

    /**
     * 创建门店（含默认配置）。
     */
    @PostMapping
    public ApiResponse<Void> create(@RequestBody CreateStoreCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeFacade.createStore(command);
        return ApiResponse.success();
    }

    /**
     * 更新门店基础信息（名称、地址、logo 等）。
     * 前端需传递 configVersion 做乐观锁控制。
     */
    @PutMapping("/base")
    public ApiResponse<Void> updateBase(@RequestBody UpdateStoreBaseCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeFacade.updateStoreBase(command);
        return ApiResponse.success();
    }

    /**
     * 批量更新门店能力配置（堂食/外卖/自取/预约等）。
     */
    @PutMapping("/capabilities")
    public ApiResponse<Void> updateCapabilities(@RequestBody UpdateStoreCapabilitiesCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeFacade.updateCapabilities(command);
        return ApiResponse.success();
    }

    /**
     * 更新常规营业时间。
     */
    @PutMapping("/opening-hours")
    public ApiResponse<Void> updateOpeningHours(@RequestBody UpdateStoreOpeningHoursCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeFacade.updateOpeningHours(command);
        return ApiResponse.success();
    }

    /**
     * 更新特殊日配置。
     */
    @PutMapping("/special-days")
    public ApiResponse<Void> updateSpecialDays(@RequestBody UpdateStoreSpecialDaysCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeFacade.updateSpecialDays(command);
        return ApiResponse.success();
    }

    /**
     * 切换门店状态（OPEN / PAUSED / CLOSED）。
     */
    @PutMapping("/status")
    public ApiResponse<Void> changeStatus(@RequestBody ChangeStoreStatusCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeFacade.changeStatus(command);
        return ApiResponse.success();
    }

    /**
     * 切换接单开关。
     */
    @PutMapping("/open-for-orders")
    public ApiResponse<Void> toggleOpenForOrders(@RequestBody ToggleOpenForOrdersCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeFacade.toggleOpenForOrders(command);
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
