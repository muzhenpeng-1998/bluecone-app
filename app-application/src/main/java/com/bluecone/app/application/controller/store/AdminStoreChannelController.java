package com.bluecone.app.application.controller.store;

import com.bluecone.app.api.ApiResponse;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.infra.tenant.TenantContext;
import com.bluecone.app.store.api.StoreChannelFacade;
import com.bluecone.app.store.api.dto.StoreChannelView;
import com.bluecone.app.store.application.command.BindStoreChannelCommand;
import com.bluecone.app.store.application.command.ChangeStoreChannelStatusCommand;
import com.bluecone.app.store.application.command.UnbindStoreChannelCommand;
import com.bluecone.app.store.application.command.UpdateStoreChannelConfigCommand;
import com.bluecone.app.store.application.query.StoreChannelListQuery;
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
 * 门店渠道管理后台 Controller。
 * <p>职责：配置门店与外部渠道（小程序/外卖等）的绑定关系。</p>
 */
@RestController
@RequestMapping("/api/admin/store/channel")
public class AdminStoreChannelController {

    private final StoreChannelFacade storeChannelFacade;

    public AdminStoreChannelController(StoreChannelFacade storeChannelFacade) {
        this.storeChannelFacade = storeChannelFacade;
    }

    /**
     * 查询指定门店的渠道绑定列表。
     */
    @GetMapping("/list")
    public ApiResponse<List<StoreChannelView>> list(@RequestParam Long storeId,
                                                    @RequestParam(required = false) String channelType,
                                                    @RequestParam(required = false) String status) {
        Long tenantId = requireTenantId();
        StoreChannelListQuery query = new StoreChannelListQuery();
        query.setTenantId(tenantId);
        query.setStoreId(storeId);
        query.setChannelType(channelType);
        query.setStatus(status);
        return ApiResponse.success(storeChannelFacade.list(query));
    }

    /**
     * 查看单个渠道绑定详情。
     */
    @GetMapping("/detail")
    public ApiResponse<StoreChannelView> detail(@RequestParam Long storeId,
                                                @RequestParam Long channelId) {
        Long tenantId = requireTenantId();
        return ApiResponse.success(storeChannelFacade.getById(tenantId, storeId, channelId));
    }

    /**
     * 为门店绑定新的外部渠道。
     */
    @PostMapping("/bind")
    public ApiResponse<Void> bind(@RequestBody BindStoreChannelCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeChannelFacade.bindChannel(command);
        return ApiResponse.success();
    }

    /**
     * 更新渠道配置 JSON。
     */
    @PutMapping("/config")
    public ApiResponse<Void> updateConfig(@RequestBody UpdateStoreChannelConfigCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeChannelFacade.updateChannelConfig(command);
        return ApiResponse.success();
    }

    /**
     * 修改渠道状态（启用/停用）。
     */
    @PutMapping("/status")
    public ApiResponse<Void> changeStatus(@RequestBody ChangeStoreChannelStatusCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeChannelFacade.changeChannelStatus(command);
        return ApiResponse.success();
    }

    /**
     * 解绑渠道（逻辑删除）。
     */
    @DeleteMapping
    public ApiResponse<Void> unbind(@RequestBody UnbindStoreChannelCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeChannelFacade.unbindChannel(command);
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
