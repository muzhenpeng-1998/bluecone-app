package com.bluecone.app.controller.order;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.order.application.OrderPreCheckService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 订单前置校验 Controller（用于调试和管理端接口）。
 * <p>提供订单提交前的门店接单能力校验接口，便于前端和运营人员进行测试和验证。</p>
 */
@Tag(name = "👤 C端开放接口 > 订单相关", description = "订单预检查接口")
@RestController
@RequestMapping("/api/orders/precheck")
@RequiredArgsConstructor
public class OrderPreCheckController {

    private final OrderPreCheckService orderPreCheckService;

    /**
     * 订单提交前置校验接口。
     * <p>校验门店是否可接单，如果不可接单，返回错误信息和 reasonCode。</p>
     *
     * @param request 前置校验请求
     * @return 校验结果
     */
    @PostMapping
    public ResponseEntity<PreCheckResponse> preCheck(@RequestBody PreCheckRequest request) {
        // 1. 获取租户上下文
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "未登录或租户上下文缺失");
        }
        Long tenantId = Long.parseLong(tenantIdStr);

        // 2. 调用 OrderPreCheckService 进行校验
        PreCheckResponse response = new PreCheckResponse();
        try {
            orderPreCheckService.preCheck(
                tenantId, 
                request.getStoreId(), 
                request.getChannelType(),
                request.getExpectedOrderTime(),
                null
            );
            // 如果没有抛出异常，说明可以接单
            response.setCanAcceptOrder(true);
            response.setMessage("门店可接单");
        } catch (BusinessException e) {
            // 如果抛出业务异常，说明不可接单
            response.setCanAcceptOrder(false);
            response.setReasonCode(e.getCode());
            response.setMessage(e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 前置校验请求 DTO。
     */
    @Data
    public static class PreCheckRequest {
        /**
         * 门店 ID（内部 ID）。
         */
        private Long storeId;

        /**
         * 渠道类型（可选）。
         */
        private String channelType;

        /**
         * 预计下单时间（可选，用于提前校验）。
         */
        private LocalDateTime expectedOrderTime;
    }

    /**
     * 前置校验响应 DTO。
     */
    @Data
    public static class PreCheckResponse {
        /**
         * 是否可接单。
         */
        private boolean canAcceptOrder;

        /**
         * 不可接单原因码（可接单时为 null）。
         */
        private String reasonCode;

        /**
         * 提示信息。
         */
        private String message;
    }
}
