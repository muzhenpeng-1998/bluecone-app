package com.bluecone.app.controller.order;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.order.application.OrderPreCheckService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 订单前置校验 Controller（用于调试和管理端接口）。
 * <p>提供订单提交前的门店接单能力校验接口，便于前端和运营人员进行测试和验证。</p>
 */
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
        Long tenantId = request.getTenantId() != null ? request.getTenantId() : requireTenantId();
        
        if (request.getStoreId() == null) {
            return ResponseEntity.badRequest()
                    .body(PreCheckResponse.failure("门店ID不能为空", "STORE_ID_REQUIRED"));
        }

        LocalDateTime now = request.getNow() != null ? request.getNow() : LocalDateTime.now();

        try {
            orderPreCheckService.preCheck(tenantId, request.getStoreId(), request.getChannelType(), now, null);
            return ResponseEntity.ok(PreCheckResponse.success());
        } catch (BusinessException e) {
            // 提取 reasonCode（从异常消息中解析，格式：原因码：xxx）
            String reasonCode = extractReasonCode(e.getMessage());
            return ResponseEntity.ok(PreCheckResponse.failure(e.getMessage(), reasonCode));
        }
    }

    /**
     * 从上下文获取租户 ID（辅助方法）。
     */
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

    /**
     * 从异常消息中提取 reasonCode。
     * 异常消息格式：门店当前不可接单：xxx（原因码：STORE_NOT_ACCEPTING_ORDERS）
     */
    private String extractReasonCode(String message) {
        if (message == null) {
            return "UNKNOWN";
        }
        int start = message.indexOf("（原因码：");
        if (start > 0) {
            int end = message.indexOf("）", start);
            if (end > start) {
                return message.substring(start + 5, end);
            }
        }
        return "UNKNOWN";
    }

    @Data
    public static class PreCheckRequest {
        private Long tenantId;
        private Long storeId;
        private String channelType;
        private LocalDateTime now;
    }

    @Data
    public static class PreCheckResponse {
        private boolean acceptable;
        private String reasonCode;
        private String reasonMessage;

        public static PreCheckResponse success() {
            PreCheckResponse response = new PreCheckResponse();
            response.setAcceptable(true);
            response.setReasonCode("OK");
            response.setReasonMessage("门店可接单");
            return response;
        }

        public static PreCheckResponse failure(String message, String reasonCode) {
            PreCheckResponse response = new PreCheckResponse();
            response.setAcceptable(false);
            response.setReasonCode(reasonCode);
            response.setReasonMessage(message);
            return response;
        }
    }
}
