package com.bluecone.app.order.application.impl;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.application.OrderPaymentAppService;
import com.bluecone.app.order.application.dto.OrderPaymentResult;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderPaymentAppServiceImpl implements OrderPaymentAppService {

    private final OrderRepository orderRepository;

    @Override
    public OrderPaymentResult onPaymentSuccess(Long tenantId, Long orderId, Long payOrderId, Long paidAmount) {
        if (tenantId == null || orderId == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "tenantId/orderId 不能为空");
        }
        Order order = orderRepository.findById(tenantId, orderId);
        if (order == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单不存在");
        }
        if (order.getStatus() != OrderStatus.WAIT_PAY) {
            OrderPaymentResult result = toResult(order);
            result.setPayOrderId(payOrderId);
            return result;
        }
        order.markPaid(payOrderId, paidAmount);
        orderRepository.update(order);
        return toResult(order);
    }

    private OrderPaymentResult toResult(Order order) {
        OrderPaymentResult result = new OrderPaymentResult();
        result.setOrderId(order.getId());
        result.setStatus(order.getStatus() != null ? order.getStatus().getCode() : null);
        result.setPayStatus(order.getPayStatus() != null ? order.getPayStatus().getCode() : null);
        result.setPayableAmount(order.getPayableAmount());
        result.setPayOrderId(extractPayOrderId(order));
        return result;
    }

    private Long extractPayOrderId(Order order) {
        if (order.getExt() == null || order.getExt().isEmpty()) {
            return null;
        }
        Object value = order.getExt().get("payOrderId");
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.valueOf((String) value);
            } catch (NumberFormatException ignore) {
            }
        }
        return null;
    }
}
