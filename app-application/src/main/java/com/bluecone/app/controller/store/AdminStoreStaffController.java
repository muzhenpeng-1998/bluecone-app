package com.bluecone.app.controller.store;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.store.api.StoreStaffFacade;
import com.bluecone.app.store.api.dto.StoreStaffView;
import com.bluecone.app.store.application.command.AddStoreStaffCommand;
import com.bluecone.app.store.application.command.BatchBindStoreStaffCommand;
import com.bluecone.app.store.application.command.ChangeStoreStaffRoleCommand;
import com.bluecone.app.store.application.command.RemoveStoreStaffCommand;
import com.bluecone.app.store.application.query.StoreStaffListQuery;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 门店员工管理后台 Controller。
 * <p>职责：维护门店与平台用户的员工关系。</p>
 */
@RestController
@RequestMapping("/api/admin/store/staff")
public class AdminStoreStaffController {

    private final StoreStaffFacade storeStaffFacade;

    public AdminStoreStaffController(StoreStaffFacade storeStaffFacade) {
        this.storeStaffFacade = storeStaffFacade;
    }

    /**
     * 查询门店员工列表。
     */
    @GetMapping("/list")
    public ApiResponse<List<StoreStaffView>> list(@RequestParam Long storeId,
                                                  @RequestParam(required = false) Long userId,
                                                  @RequestParam(required = false) String role) {
        Long tenantId = requireTenantId();
        StoreStaffListQuery query = new StoreStaffListQuery();
        query.setTenantId(tenantId);
        query.setStoreId(storeId);
        query.setUserId(userId);
        query.setRole(role);
        return ApiResponse.success(storeStaffFacade.list(query));
    }

    /**
     * 为门店新增一名员工。
     */
    @PostMapping
    public ApiResponse<Void> add(@RequestBody AddStoreStaffCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeStaffFacade.addStaff(command);
        return ApiResponse.success();
    }

    /**
     * 移除门店员工。
     */
    @DeleteMapping
    public ApiResponse<Void> remove(@RequestBody RemoveStoreStaffCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeStaffFacade.removeStaff(command);
        return ApiResponse.success();
    }

    /**
     * 调整门店员工角色。
     */
    @PutMapping("/role")
    public ApiResponse<Void> changeRole(@RequestBody ChangeStoreStaffRoleCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeStaffFacade.changeRole(command);
        return ApiResponse.success();
    }

    /**
     * 批量绑定门店员工。
     */
    @PostMapping("/batch-bind")
    public ApiResponse<Void> batchBind(@RequestBody BatchBindStoreStaffCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeStaffFacade.batchBindStaff(command);
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
