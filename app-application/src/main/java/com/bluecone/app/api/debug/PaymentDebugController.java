package com.bluecone.app.application.payment.debug;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.order.application.OrderPaymentAppService;
import com.bluecone.app.order.application.dto.OrderPaymentResult;
import com.bluecone.app.payment.simple.application.PaymentCommandAppService;
import com.bluecone.app.payment.simple.application.dto.PaymentOrderDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部调试用的支付成功模拟接口，联动订单与支付状态。
 */
@Tag(name = "🛠️ 开发调试 > 支付调试", description = "支付模拟调试接口")
@RestController
@RequestMapping("/api/payment/debug")
public class PaymentDebugController {

    private final PaymentCommandAppService paymentCommandAppService;
    private final OrderPaymentAppService orderPaymentAppService;

    public PaymentDebugController(PaymentCommandAppService paymentCommandAppService,
                                  OrderPaymentAppService orderPaymentAppService) {
        this.paymentCommandAppService = paymentCommandAppService;
        this.orderPaymentAppService = orderPaymentAppService;
    }

    /**
     * 模拟支付成功，并把订单从 WAIT_PAY 流转到 WAIT_ACCEPT。
     */
    @PostMapping("/pay-orders/{payOrderId}/mock-paid")
    public ApiResponse<MockPaySuccessResponse> mockPaymentSuccess(
            @PathVariable("payOrderId") Long payOrderId,
            @RequestParam("tenantId") Long tenantId) {
        PaymentOrderDTO paymentOrder = paymentCommandAppService.markPaid(tenantId, payOrderId);
        OrderPaymentResult orderView = orderPaymentAppService.onPaymentSuccess(
                tenantId,
                paymentOrder.getOrderId(),
                paymentOrder.getId(),
                paymentOrder.getPaidAmount());
        MockPaySuccessResponse resp = MockPaySuccessResponse.from(paymentOrder, orderView);
        return ApiResponse.success(resp);
    }
}
