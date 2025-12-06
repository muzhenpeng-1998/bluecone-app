package com.bluecone.app.controller.mini.order;

import com.bluecone.app.api.ApiResponse;
import com.bluecone.app.controller.mini.order.dto.SubmitOrderFromDraftRequest;
import com.bluecone.app.infra.redis.idempotent.IdempotentScene;
import com.bluecone.app.infra.redis.idempotent.annotation.Idempotent;
import com.bluecone.app.order.api.order.OrderSubmitFacade;
import com.bluecone.app.order.api.order.dto.OrderSubmitResponse;
import com.bluecone.app.order.api.order.dto.SubmitOrderFromDraftDTO;
import com.bluecone.app.infra.tenant.TenantContext;
import com.bluecone.app.user.application.CurrentUserContext;
import jakarta.validation.Valid;
import java.util.function.Supplier;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信小程序订单提交接口。
 */
@RestController
@RequestMapping("/api/mini/orders")
public class MiniOrderController {

    private static final String CHANNEL = "WECHAT_MINI";
    private static final String DEFAULT_SCENE = "DINE_IN";

    private final OrderSubmitFacade orderSubmitFacade;
    private final CurrentUserContext currentUserContext;

    public MiniOrderController(OrderSubmitFacade orderSubmitFacade,
                               CurrentUserContext currentUserContext) {
        this.orderSubmitFacade = orderSubmitFacade;
        this.currentUserContext = currentUserContext;
    }

    /**
     * 提交草稿订单，幂等 key 包含 orderToken。
     */
    @PostMapping("/submit")
    @Idempotent(key = "#root.target.buildSubmitKey(#request)", scene = IdempotentScene.API, expireSeconds = 300)
    public ApiResponse<OrderSubmitResponse> submitOrder(@Valid @RequestBody SubmitOrderFromDraftRequest request) {
        return withMdc(request.getStoreId(), request.getScene(), () -> ApiResponse.success(orderSubmitFacade.submitOrderFromCurrentDraft(toCommand(request))));
    }

    private SubmitOrderFromDraftDTO toCommand(SubmitOrderFromDraftRequest request) {
        SubmitOrderFromDraftDTO dto = new SubmitOrderFromDraftDTO();
        dto.setTenantId(currentTenantId());
        dto.setStoreId(request.getStoreId());
        dto.setUserId(currentUserContext.getCurrentUserId());
        dto.setChannel(CHANNEL);
        dto.setScene(StringUtils.hasText(request.getScene()) ? request.getScene() : DEFAULT_SCENE);
        dto.setOrderToken(request.getOrderToken());
        dto.setClientPayableAmount(request.getClientPayableAmount());
        dto.setUserRemark(request.getUserRemark());
        dto.setContactName(request.getContactName());
        dto.setContactPhone(request.getContactPhone());
        dto.setAddressJson(request.getAddressJson());
        return dto;
    }

    public String buildSubmitKey(SubmitOrderFromDraftRequest request) {
        Long tenantId = currentTenantId();
        Long storeId = request.getStoreId();
        Long userId = currentUserContext.getCurrentUserId();
        return "mini:order:submit:" + safe(tenantId) + ":" + safe(storeId) + ":" + safe(userId) + ":" + safe(request.getOrderToken());
    }

    private ApiResponse<OrderSubmitResponse> withMdc(Long storeId, String scene, Supplier<ApiResponse<OrderSubmitResponse>> supplier) {
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
        MDC.put("scene", StringUtils.hasText(scene) ? scene : DEFAULT_SCENE);
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

    private String safe(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }
}
