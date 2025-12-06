package com.bluecone.app.order.application.impl;

import com.bluecone.app.order.api.dto.UserOrderRefundRequest;
import com.bluecone.app.order.application.UserOrderRefundAppService;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.domain.enums.PayStatus;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.model.OrderPayment;
import com.bluecone.app.order.domain.repository.OrderPaymentRepository;
import com.bluecone.app.order.domain.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserOrderRefundAppServiceImpl implements UserOrderRefundAppService {

    private final OrderRepository orderRepository;
    private final OrderPaymentRepository orderPaymentRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(Long orderId, UserOrderRefundRequest request) {
        if (orderId == null || request.getTenantId() == null || request.getUserId() == null) {
            throw new IllegalArgumentException("tenantId/userId/orderId 不能为空");
        }
        Long tenantId = request.getTenantId();
        Long userId = request.getUserId();

        Order order = orderRepository.findById(tenantId, orderId);
        if (order == null) {
            log.warn("User apply refund but order not found, tenantId={}, userId={}, orderId={}", tenantId, userId, orderId);
            throw new IllegalStateException("订单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            log.warn("User apply refund but not owner, tenantId={}, userId={}, orderUserId={}, orderId={}",
                    tenantId, userId, order.getUserId(), orderId);
            throw new IllegalStateException("无权操作该订单");
        }

        PayStatus payStatus = order.getPayStatus();
        if (payStatus == null || payStatus == PayStatus.UNPAID || payStatus == PayStatus.INIT) {
            throw new IllegalStateException("未支付订单不可退款");
        }
        if (payStatus == PayStatus.REFUNDING || payStatus == PayStatus.REFUNDED) {
            log.info("User apply refund but already in refund flow, tenantId={}, userId={}, orderId={}", tenantId, userId, orderId);
            return;
        }
        if (!isRefundableOrderStatus(order.getStatus())) {
            throw new IllegalStateException("当前状态不允许退款");
        }

        BigDecimal refundAmount = calcRefundAmount(order.getPayableAmount(), request);

        OrderPayment payment = orderPaymentRepository.findByOrderId(tenantId, orderId);
        if (payment == null) {
            log.error("User apply refund but payment not found, tenantId={}, userId={}, orderId={}", tenantId, userId, orderId);
            throw new IllegalStateException("订单支付信息不存在");
        }
        payment.setPayStatus(PayStatus.REFUNDING);
        Map<String, Object> extra = new java.util.HashMap<>();
        if (payment.getExtra() != null) {
            extra.putAll(payment.getExtra());
        }
        extra.put("refundType", request.getRefundType());
        extra.put("refundAmount", refundAmount);
        extra.put("reasonCode", request.getReasonCode());
        extra.put("reasonRemark", request.getReasonRemark());
        payment.setExtra(extra);
        orderPaymentRepository.updateStatus(payment);

        order.setPayStatus(PayStatus.REFUNDING);
        orderRepository.update(order);

        log.info("User apply refund success, tenantId={}, userId={}, orderId={}, refundType={}, refundAmount={}",
                tenantId, userId, orderId, request.getRefundType(), refundAmount);
    }

    private boolean isRefundableOrderStatus(OrderStatus status) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case COMPLETED, READY, PENDING_ACCEPT, IN_PROGRESS -> true;
            default -> false;
        };
    }

    private BigDecimal calcRefundAmount(BigDecimal payableAmount, UserOrderRefundRequest request) {
        BigDecimal payable = payableAmount == null ? BigDecimal.ZERO : payableAmount;
        if ("FULL".equalsIgnoreCase(request.getRefundType())) {
            return payable;
        }
        BigDecimal req = request.getRefundAmount();
        if (req == null || req.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("部分退款金额必须大于0");
        }
        if (req.compareTo(payable) > 0) {
            throw new IllegalStateException("退款金额不能大于实付金额");
        }
        return req;
    }
}
