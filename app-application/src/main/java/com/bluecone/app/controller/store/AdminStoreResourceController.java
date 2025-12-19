package com.bluecone.app.controller.store;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.store.api.StoreResourceFacade;
import com.bluecone.app.store.api.dto.StoreResourceView;
import com.bluecone.app.store.application.command.ChangeStoreResourceStatusCommand;
import com.bluecone.app.store.application.command.CreateStoreResourceCommand;
import com.bluecone.app.store.application.command.UpdateStoreResourceCommand;
import com.bluecone.app.store.application.query.StoreResourceListQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 门店资源管理后台 Controller。
 * <p>职责：管理门店内部资源（餐桌/包间/场地等）。</p>
 */
@RestController
@RequestMapping("/api/admin/store/resource")
public class AdminStoreResourceController {

    private final StoreResourceFacade storeResourceFacade;

    public AdminStoreResourceController(StoreResourceFacade storeResourceFacade) {
        this.storeResourceFacade = storeResourceFacade;
    }

    /**
     * 查询门店资源列表。
     */
    @GetMapping("/list")
    public ApiResponse<List<StoreResourceView>> list(@RequestParam Long storeId,
                                                     @RequestParam(required = false) String resourceType,
                                                     @RequestParam(required = false) String area,
                                                     @RequestParam(required = false) String status) {
        Long tenantId = requireTenantId();
        StoreResourceListQuery query = new StoreResourceListQuery();
        query.setTenantId(tenantId);
        query.setStoreId(storeId);
        query.setResourceType(resourceType);
        query.setArea(area);
        query.setStatus(status);
        return ApiResponse.success(storeResourceFacade.list(query));
    }

    /**
     * 查看单个资源详情。
     */
    @GetMapping("/detail")
    public ApiResponse<StoreResourceView> detail(@RequestParam Long storeId,
                                                 @RequestParam Long resourceId) {
        Long tenantId = requireTenantId();
        return ApiResponse.success(storeResourceFacade.getById(tenantId, storeId, resourceId));
    }

    /**
     * 创建门店资源。
     */
    @PostMapping
    public ApiResponse<Void> create(@RequestBody CreateStoreResourceCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeResourceFacade.createResource(command);
        return ApiResponse.success();
    }

    /**
     * 更新门店资源。
     */
    @PutMapping
    public ApiResponse<Void> update(@RequestBody UpdateStoreResourceCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeResourceFacade.updateResource(command);
        return ApiResponse.success();
    }

    /**
     * 修改资源状态（启用/停用/移除）。
     */
    @PutMapping("/status")
    public ApiResponse<Void> changeStatus(@RequestBody ChangeStoreResourceStatusCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeResourceFacade.changeStatus(command);
        return ApiResponse.success();
    }

    private Long requireTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null || tenantIdStr.isBlank()) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "租户未登录或上下文缺失");
        }
        try {
            return Long.parseLong(tenantIdStr);
        } catch (NumberFormatException ex) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "非法的租户标识");
        }
    }
}
