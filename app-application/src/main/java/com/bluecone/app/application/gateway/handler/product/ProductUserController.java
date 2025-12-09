package com.bluecone.app.application.gateway.handler.product;

import com.bluecone.app.api.ApiResponse;
import com.bluecone.app.product.application.UserProductQueryAppService;
import com.bluecone.app.application.gateway.dto.product.UserStoreMenuResponse;
import com.bluecone.app.product.application.dto.UserStoreMenuView;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 暴露给小程序的商品接口，目前提供门店菜单。
 */
@RestController
@RequestMapping("/api/product/user")
@RequiredArgsConstructor
public class ProductUserController {

    private final UserProductQueryAppService userProductQueryAppService;

    @GetMapping("/menu")
    public ApiResponse<UserStoreMenuResponse> getUserStoreMenu(
            @RequestParam("tenantId") Long tenantId,
            @RequestParam("storeId") Long storeId) {
        // TODO 后续从上下文/登录态获取 tenantId/storeId
        UserStoreMenuView view = userProductQueryAppService.getUserStoreMenu(tenantId, storeId);
        return ApiResponse.success(UserStoreMenuResponse.from(view));
    }
}
