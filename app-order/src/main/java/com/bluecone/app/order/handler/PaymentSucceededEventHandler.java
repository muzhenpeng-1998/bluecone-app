package com.bluecone.app.order.handler;

import com.bluecone.app.core.event.EventHandler;
import com.bluecone.app.core.event.annotations.EventHandlerComponent;
import com.bluecone.app.order.application.OrderPaymentStatusAppService;
import com.bluecone.app.payment.event.PaymentSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 接收支付域的支付成功事件，将订单标记为已支付（幂等处理）。
 */
@Slf4j
@RequiredArgsConstructor
@EventHandlerComponent
public class PaymentSucceededEventHandler implements EventHandler<PaymentSucceededEvent> {

    private final OrderPaymentStatusAppService orderPaymentStatusAppService;

    @Override
    public void handle(final PaymentSucceededEvent event) {
        Long orderId = event.getOrderId();
        Long tenantId = event.getTenantId();
        if (orderId == null || tenantId == null) {
            log.warn("[PaymentSucceededEventHandler] 缺少 tenant/order，跳过处理 eventId={} orderId={} tenantId={}",
                    event.getEventId(), orderId, tenantId);
            return;
        }
        try {
            orderPaymentStatusAppService.onPaySuccess(
                    tenantId,
                    orderId,
                    event.getPayChannel(),
                    event.getChannelTradeNo(),
                    event.getPayAmount());
            log.info("[PaymentSucceededEventHandler] 订单支付状态已更新，orderId={}, paymentOrderId={}, channel={}, amount={}, traceId={}",
                    orderId,
                    event.getPaymentOrderId(),
                    event.getPayChannel(),
                    event.getPayAmount(),
                    event.getTraceId());
        } catch (Exception ex) {
            log.error("[PaymentSucceededEventHandler] 订单支付处理失败，orderId={}, paymentOrderId={}, traceId={}",
                    orderId,
                    event.getPaymentOrderId(),
                    event.getTraceId(),
                    ex);
            throw ex;
        }
    }
}
