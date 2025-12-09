package com.bluecone.app.product.controller;

import com.bluecone.app.api.ApiResponse;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.gateway.context.RuntimeContextUtil;
import com.bluecone.app.product.application.menu.StoreMenuSnapshotApplicationService;
import com.bluecone.app.product.dto.view.StoreMenuSnapshotView;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端门店菜单查询接口，返回快照以支撑高并发读。
 */
@RestController
@RequestMapping("/api/member/store/menu")
@Validated
@RequiredArgsConstructor
public class StoreMenuController {

    private final StoreMenuSnapshotApplicationService storeMenuSnapshotApplicationService;

    /**
     * 获取门店菜单快照。
     */
    @GetMapping("/snapshot")
    public ApiResponse<StoreMenuSnapshotView> getSnapshot(@RequestParam(required = false) Long storeId,
                                                          @RequestParam(required = false, defaultValue = "ALL") String channel,
                                                          @RequestParam(required = false, defaultValue = "DEFAULT") String orderScene) {
        Long resolvedStoreId = RuntimeContextUtil.currentStoreId();
        if (resolvedStoreId == null) {
            resolvedStoreId = storeId; // 兼容旧参数
        }
        if (resolvedStoreId == null) {
            throw BusinessException.of(ErrorCode.PARAM_MISSING.getCode(), "storeId missing");
        }
        StoreMenuSnapshotView view = storeMenuSnapshotApplicationService
                .getStoreMenuSnapshot(null, resolvedStoreId, channel, orderScene);
        return ApiResponse.success(view);
    }
}
