package com.bluecone.app.controller.mini.cart;

import com.bluecone.app.api.ApiResponse;
import com.bluecone.app.controller.mini.cart.dto.AddDraftItemRequest;
import com.bluecone.app.controller.mini.cart.dto.ChangeDraftItemQuantityRequest;
import com.bluecone.app.controller.mini.cart.dto.LockDraftRequest;
import com.bluecone.app.controller.mini.cart.dto.RemoveDraftItemRequest;
import com.bluecone.app.order.api.cart.OrderDraftFacade;
import com.bluecone.app.order.api.cart.dto.AddDraftItemCommandDTO;
import com.bluecone.app.order.api.cart.dto.ChangeDraftItemQuantityCommandDTO;
import com.bluecone.app.order.api.cart.dto.ClearDraftCommandDTO;
import com.bluecone.app.order.api.cart.dto.LockDraftCommandDTO;
import com.bluecone.app.order.api.cart.dto.OrderDraftViewDTO;
import com.bluecone.app.order.api.cart.dto.RemoveDraftItemCommandDTO;
import com.bluecone.app.infra.redis.idempotent.IdempotentScene;
import com.bluecone.app.infra.redis.idempotent.annotation.Idempotent;
import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.user.application.CurrentUserContext;
import jakarta.validation.Valid;
import java.util.function.Supplier;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信小程序端购物车接口（订单草稿）。
 *
 * 读接口后续将配合 app-order 的多级缓存；写接口触发缓存失效由应用服务内部处理。
 */
@RestController
@RequestMapping("/api/mini/cart")
public class MiniCartController {

    private static final String CHANNEL = "WECHAT_MINI";
    private static final String DEFAULT_SCENE = "DINE_IN";

    private final OrderDraftFacade orderDraftFacade;
    private final CurrentUserContext currentUserContext;

    public MiniCartController(OrderDraftFacade orderDraftFacade, CurrentUserContext currentUserContext) {
        this.orderDraftFacade = orderDraftFacade;
        this.currentUserContext = currentUserContext;
    }

    /**
     * 查询当前购物车（订单草稿）。
     */
    @GetMapping
    public ApiResponse<OrderDraftViewDTO> getCurrentCart(@RequestParam("storeId") Long storeId) {
        return withMdc(storeId, () -> ApiResponse.success(orderDraftFacade.loadCurrentDraft()));
    }

    /**
     * 加入购物车，使用客户端 requestId 做幂等。
     */
    @PostMapping("/items")
    @Idempotent(key = "#root.target.buildAddKey(#request)", scene = IdempotentScene.API, expireSeconds = 300)
    public ApiResponse<OrderDraftViewDTO> addItem(@Valid @RequestBody AddDraftItemRequest request) {
        enrichContext(request);
        AddDraftItemCommandDTO command = new AddDraftItemCommandDTO();
        command.setProductId(request.getProductId());
        command.setSkuId(request.getSkuId());
        command.setQuantity(request.getQuantity());
        command.setAttrsJson(request.getAttrsJson());
        command.setRemark(request.getRemark());
        command.setClientUnitPrice(request.getClientUnitPrice());
        return withMdc(request.getStoreId(), () -> ApiResponse.success(orderDraftFacade.addItem(command)));
    }

    /**
     * 修改购物车明细数量。
     */
    @PatchMapping("/items/{skuId}/quantity")
    public ApiResponse<OrderDraftViewDTO> changeItemQuantity(@PathVariable("skuId") Long skuId,
                                                             @Valid @RequestBody ChangeDraftItemQuantityRequest request) {
        ChangeDraftItemQuantityCommandDTO command = new ChangeDraftItemQuantityCommandDTO();
        command.setSkuId(skuId);
        command.setAttrsJson(request.getAttrsJson());
        command.setNewQuantity(request.getNewQuantity());
        return withMdc(request.getStoreId(), () -> ApiResponse.success(orderDraftFacade.changeItemQuantity(command)));
    }

    /**
     * 删除购物车明细。
     */
    @DeleteMapping("/items/{skuId}")
    public ApiResponse<OrderDraftViewDTO> removeItem(@PathVariable("skuId") Long skuId,
                                                     @Valid @RequestBody RemoveDraftItemRequest request) {
        RemoveDraftItemCommandDTO command = new RemoveDraftItemCommandDTO();
        command.setSkuId(skuId);
        command.setAttrsJson(request.getAttrsJson());
        return withMdc(request.getStoreId(), () -> ApiResponse.success(orderDraftFacade.removeItem(command)));
    }

    /**
     * 清空购物车。
     */
    @DeleteMapping
    public ApiResponse<OrderDraftViewDTO> clearCart(@RequestParam("storeId") Long storeId) {
        return withMdc(storeId, () -> ApiResponse.success(orderDraftFacade.clearDraft(new ClearDraftCommandDTO())));
    }

    /**
     * 锁定购物车，准备下单，使用 orderToken 做幂等。
     */
    @PostMapping("/lock")
    @Idempotent(key = "#root.target.buildLockKey(#request)", scene = IdempotentScene.API, expireSeconds = 300)
    public ApiResponse<OrderDraftViewDTO> lockCart(@Valid @RequestBody LockDraftRequest request) {
        LockDraftCommandDTO command = new LockDraftCommandDTO();
        command.setClientPayableAmount(request.getClientPayableAmount());
        command.setOrderToken(request.getOrderToken());
        return withMdc(request.getStoreId(), () -> ApiResponse.success(orderDraftFacade.lockDraft(command)));
    }

    /**
     * 生成加购幂等键（包含租户/门店/用户）。
     */
    public String buildAddKey(AddDraftItemRequest request) {
        Long tenantId = currentTenantId();
        Long userId = currentUserContext.getCurrentUserId();
        Long storeId = request.getStoreId();
        return "mini:cart:add:" + safe(tenantId) + ":" + safe(storeId) + ":" + safe(userId) + ":" + safe(request.getClientRequestId());
    }

    /**
     * 生成锁单幂等键。
     */
    public String buildLockKey(LockDraftRequest request) {
        Long tenantId = currentTenantId();
        Long userId = currentUserContext.getCurrentUserId();
        Long storeId = request.getStoreId();
        return "mini:cart:lock:" + safe(tenantId) + ":" + safe(storeId) + ":" + safe(userId) + ":" + safe(request.getOrderToken());
    }

    private void enrichContext(AddDraftItemRequest request) {
        request.setTenantId(currentTenantId());
        request.setUserId(currentUserContext.getCurrentUserId());
    }

    private <T> T withMdc(Long storeId, Supplier<T> supplier) {
        String prevStore = MDC.get("storeId");
        String prevUser = MDC.get("userId");
        String prevChannel = MDC.get("channel");
        String prevScene = MDC.get("scene");
        if (storeId != null) {
            MDC.put("storeId", String.valueOf(storeId));
        }
        Long userId = currentUserContext.getCurrentUserId();
        if (userId != null) {
            MDC.put("userId", String.valueOf(userId));
        }
        MDC.put("channel", CHANNEL);
        MDC.put("scene", DEFAULT_SCENE);
        try {
            return supplier.get();
        } finally {
            restoreMdc("storeId", prevStore);
            restoreMdc("userId", prevUser);
            restoreMdc("channel", prevChannel);
            restoreMdc("scene", prevScene);
        }
    }

    private void restoreMdc(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    private Long currentTenantId() {
        String tenantRaw = TenantContext.getTenantId();
        if (!StringUtils.hasText(tenantRaw)) {
            return null;
        }
        try {
            return Long.valueOf(tenantRaw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String safe(Object val) {
        return val == null ? "null" : String.valueOf(val);
    }
}
