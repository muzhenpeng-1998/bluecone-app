package com.bluecone.app.application.controller.store;

import com.bluecone.app.api.ApiResponse;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.infra.tenant.TenantContext;
import com.bluecone.app.store.api.StoreFacade;
import com.bluecone.app.store.api.dto.StoreBaseView;
import com.bluecone.app.store.api.dto.StoreOrderAcceptResult;
import com.bluecone.app.store.api.dto.StoreOrderSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 门店开放接口 Controller。
 * <p>职责：为小程序/H5/三方提供门店运行时查询能力，读链路依赖 StoreFacade 内部的多级缓存与规则。</p>
 */
@RestController
@RequestMapping("/api/open/store")
public class OpenStoreController {

    private final StoreFacade storeFacade;

    public OpenStoreController(StoreFacade storeFacade) {
        this.storeFacade = storeFacade;
    }

    /**
     * 获取门店基础信息视图（给 C 端展示）。
     */
    @GetMapping("/base")
    public ApiResponse<StoreBaseView> getBase(@RequestParam Long storeId) {
        Long tenantId = requireTenantId();
        StoreBaseView view = storeFacade.getStoreBase(tenantId, storeId);
        return ApiResponse.success(view);
    }

    /**
     * 获取订单视角快照，下单前可先调用用于前端展示和本地校验。
     */
    @GetMapping("/order-snapshot")
    public ApiResponse<StoreOrderSnapshot> getOrderSnapshot(@RequestParam Long storeId,
                                                            @RequestParam(required = false) String channelType) {
        Long tenantId = requireTenantId();
        LocalDateTime now = LocalDateTime.now();
        StoreOrderSnapshot snapshot = storeFacade.getOrderSnapshot(tenantId, storeId, now, channelType);
        return ApiResponse.success(snapshot);
    }

    /**
     * 检查当前是否允许接指定类型订单，订单模块可作为后端兜底校验。
     */
    @GetMapping("/check-acceptable")
    public ApiResponse<StoreOrderAcceptResult> checkAcceptable(@RequestParam Long storeId,
                                                               @RequestParam String capability,
                                                               @RequestParam(required = false) String channelType) {
        Long tenantId = requireTenantId();
        LocalDateTime now = LocalDateTime.now();
        StoreOrderAcceptResult result = storeFacade.checkOrderAcceptable(tenantId, storeId, capability, now, channelType);
        return ApiResponse.success(result);
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
