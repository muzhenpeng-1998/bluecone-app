package com.bluecone.app.api.admin.system;

import com.bluecone.app.core.notify.NotificationFacade;
import com.bluecone.app.core.notify.NotificationRequest;
import com.bluecone.app.core.notify.NotificationResponse;
import com.bluecone.app.core.notify.NotificationScenario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/**
 * 通知平台管理/调试入口（仅示范）。
 */
@Tag(name = "🎛️ 平台管理后台 > 系统管理 > 通知管理", description = "平台后台通知管理接口（测试/调试）")
@RestController
@RequestMapping("/api/admin/notify")
public class NotifyAdminController {

    private static final Logger log = LoggerFactory.getLogger(NotifyAdminController.class);

    private final NotificationFacade notificationFacade;

    public NotifyAdminController(NotificationFacade notificationFacade) {
        this.notificationFacade = Objects.requireNonNull(notificationFacade, "notificationFacade must not be null");
    }

    @PostMapping("/test/order-paid")
    public NotificationResponse testOrderPaid(@RequestBody OrderPaidRequest request) {
        NotificationRequest notifyRequest = new NotificationRequest(
                NotificationScenario.ORDER_PAID_SHOP_OWNER.getCode(),
                request.getTenantId(),
                request.getOperatorUserId(),
                NotificationScenario.ORDER_PAID_SHOP_OWNER.getDefaultPriority(),
                Map.of(
                        "orderId", request.getOrderId(),
                        "amount", request.getAmount()
                ),
                null
        );
        return notificationFacade.send(notifyRequest);
    }

    @PostMapping("/test/wechat")
    public NotificationResponse testWeChat(@RequestBody WeChatTestRequest request) {
        NotificationRequest notifyRequest = new NotificationRequest(
                request.getScenarioCode(),
                request.getTenantId(),
                request.getUserId(),
                request.getPriority(),
                Map.of("content", request.getContent()),
                request.getIdempotentKey()
        );
        NotificationResponse response = notificationFacade.send(notifyRequest);
        log.info("[NotifyAdmin] wechat test response={} requestId={}", response.isAccepted(), response.getRequestId());
        return response;
    }

    public static class OrderPaidRequest {
        private Long tenantId;
        private Long orderId;
        private BigDecimal amount;
        private Long operatorUserId;

        public Long getTenantId() {
            return tenantId;
        }

        public void setTenantId(Long tenantId) {
            this.tenantId = tenantId;
        }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public Long getOperatorUserId() {
            return operatorUserId;
        }

        public void setOperatorUserId(Long operatorUserId) {
            this.operatorUserId = operatorUserId;
        }
    }

    public static class WeChatTestRequest {
        private String scenarioCode = NotificationScenario.SYSTEM_ERROR_PLATFORM_OPS.getCode();
        private Long tenantId;
        private Long userId;
        private com.bluecone.app.core.notify.NotificationPriority priority = com.bluecone.app.core.notify.NotificationPriority.NORMAL;
        private String content;
        private String idempotentKey;

        public String getScenarioCode() {
            return scenarioCode;
        }

        public void setScenarioCode(String scenarioCode) {
            this.scenarioCode = scenarioCode;
        }

        public Long getTenantId() {
            return tenantId;
        }

        public void setTenantId(Long tenantId) {
            this.tenantId = tenantId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public com.bluecone.app.core.notify.NotificationPriority getPriority() {
            return priority;
        }

        public void setPriority(com.bluecone.app.core.notify.NotificationPriority priority) {
            this.priority = priority;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getIdempotentKey() {
            return idempotentKey;
        }

        public void setIdempotentKey(String idempotentKey) {
            this.idempotentKey = idempotentKey;
        }
    }
}
