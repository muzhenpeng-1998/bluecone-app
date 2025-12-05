package com.bluecone.app.product.controller;

import com.bluecone.app.api.ApiResponse;
import com.bluecone.app.product.application.command.RebuildStoreMenuSnapshotCommand;
import com.bluecone.app.product.application.query.StoreMenuSnapshotQuery;
import com.bluecone.app.product.application.service.StoreMenuApplicationService;
import com.bluecone.app.product.dto.StoreMenuSnapshotDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 门店菜单快照接口：读写分离，GET 为高并发读取，POST 重建仅在菜单更新时调用。
 */
@RestController
@RequestMapping("/api/product/store-menu")
@Validated
@RequiredArgsConstructor
public class ProductStoreMenuController {

    private final StoreMenuApplicationService storeMenuApplicationService;

    /**
     * 小程序/前端读取门店菜单快照，直接命中快照表。
     */
    @GetMapping
    public ApiResponse<StoreMenuSnapshotDTO> getSnapshot(@RequestParam Long storeId,
                                                         @RequestParam(required = false, defaultValue = "ALL") String channel,
                                                         @RequestParam(required = false, defaultValue = "DEFAULT") String orderScene) {
        StoreMenuSnapshotQuery query = StoreMenuSnapshotQuery.builder()
                .storeId(storeId)
                .channel(channel)
                .orderScene(orderScene)
                .build();
        StoreMenuSnapshotDTO dto = storeMenuApplicationService.getSnapshot(query);
        return ApiResponse.success(dto);
    }

    /**
     * 管理端触发菜单快照重建。
     */
    @PostMapping("/rebuild")
    public ApiResponse<StoreMenuSnapshotDTO> rebuild(@RequestBody RebuildStoreMenuSnapshotCommand command) {
        StoreMenuSnapshotDTO dto = storeMenuApplicationService.rebuildSnapshot(command);
        return ApiResponse.success(dto);
    }
}
