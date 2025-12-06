package com.bluecone.app.order.application.impl;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.application.OrderPaymentStatusAppService;
import com.bluecone.app.order.domain.enums.BizType;
import com.bluecone.app.order.domain.enums.OrderEvent;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.domain.enums.PayStatus;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.model.OrderPayment;
import com.bluecone.app.order.domain.repository.OrderPaymentRepository;
import com.bluecone.app.order.domain.repository.OrderRepository;
import com.bluecone.app.order.domain.service.OrderStateMachine;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPaymentStatusAppServiceImpl implements OrderPaymentStatusAppService {

    private final OrderRepository orderRepository;
    private final OrderPaymentRepository orderPaymentRepository;
    private final OrderStateMachine orderStateMachine;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onPaySuccess(Long tenantId, Long orderId, String payChannel, String thirdTradeNo, BigDecimal payAmount) {
        if (tenantId == null || orderId == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "tenantId/orderId 不能为空");
        }
        Order order = orderRepository.findById(tenantId, orderId);
        if (order == null) {
            log.warn("onPaySuccess: order not found, tenantId={}, orderId={}, channel={}, tradeNo={}",
                    tenantId, orderId, payChannel, thirdTradeNo);
            return;
        }
        OrderPayment payment = orderPaymentRepository.findByOrderId(tenantId, orderId);
        if (payment == null) {
            log.error("onPaySuccess: payment not found, tenantId={}, orderId={}, channel={}, tradeNo={}",
                    tenantId, orderId, payChannel, thirdTradeNo);
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单支付记录不存在");
        }

        PayStatus payStatus = payment.getPayStatus();
        if (payStatus == PayStatus.PAID || payStatus == PayStatus.REFUNDING || payStatus == PayStatus.REFUNDED) {
            log.info("onPaySuccess: already processed, tenantId={}, orderId={}, payStatus={}, channel={}, tradeNo={}",
                    tenantId, orderId, payStatus, payChannel, thirdTradeNo);
            return;
        }

        BigDecimal payableAmount = order.getPayableAmount();
        if (payAmount != null && payableAmount != null && payAmount.compareTo(payableAmount) != 0) {
            log.warn("onPaySuccess: payAmount mismatch, tenantId={}, orderId={}, payableAmount={}, payAmount={}",
                    tenantId, orderId, payableAmount, payAmount);
        }

        OrderStatus currentStatus = order.getStatus();
        BizType bizType = order.getBizType();
        OrderStatus nextStatus = orderStateMachine.transitOrThrow(bizType, currentStatus, OrderEvent.PAY_SUCCESS);
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(nextStatus);
        order.setPayStatus(PayStatus.PAID);
        order.setUpdatedAt(now);

        payment.setPayStatus(PayStatus.PAID);
        payment.setPayChannel(payChannel);
        if (payAmount != null) {
            payment.setPayAmount(payAmount);
        }
        payment.setThirdTradeNo(thirdTradeNo);
        payment.setPayTime(now);
        payment.setUpdatedAt(now);
        orderPaymentRepository.updateStatus(payment);

        orderRepository.update(order);

        log.info("onPaySuccess: state changed via state machine, tenantId={}, orderId={}, fromStatus={}, toStatus={}, channel={}, tradeNo={}",
                tenantId, orderId, currentStatus, nextStatus, payChannel, thirdTradeNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onPayFailed(Long tenantId, Long orderId, String payChannel, String thirdTradeNo) {
        if (tenantId == null || orderId == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "tenantId/orderId 不能为空");
        }
        Order order = orderRepository.findById(tenantId, orderId);
        if (order == null) {
            log.warn("onPayFailed: order not found, tenantId={}, orderId={}, channel={}, tradeNo={}",
                    tenantId, orderId, payChannel, thirdTradeNo);
            return;
        }
        OrderPayment payment = orderPaymentRepository.findByOrderId(tenantId, orderId);
        if (payment == null) {
            log.error("onPayFailed: payment not found, tenantId={}, orderId={}, channel={}, tradeNo={}",
                    tenantId, orderId, payChannel, thirdTradeNo);
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单支付记录不存在");
        }

        OrderStatus currentStatus = order.getStatus();
        if (currentStatus == OrderStatus.CANCELLED || currentStatus == OrderStatus.COMPLETED || currentStatus == OrderStatus.REFUNDED) {
            log.info("onPayFailed: order already terminal, tenantId={}, orderId={}, status={}, channel={}, tradeNo={}",
                    tenantId, orderId, currentStatus, payChannel, thirdTradeNo);
            return;
        }
        PayStatus payStatus = payment.getPayStatus();
        if (payStatus == PayStatus.PAID || payStatus == PayStatus.REFUNDING || payStatus == PayStatus.REFUNDED) {
            log.info("onPayFailed: payment already processed, tenantId={}, orderId={}, payStatus={}, channel={}, tradeNo={}",
                    tenantId, orderId, payStatus, payChannel, thirdTradeNo);
            return;
        }

        BizType bizType = order.getBizType();
        OrderStatus nextStatus = orderStateMachine.transitOrThrow(bizType, currentStatus, OrderEvent.PAY_FAILED);
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(nextStatus);
        order.setPayStatus(PayStatus.UNPAID);
        order.setUpdatedAt(now);

        payment.setPayStatus(PayStatus.UNPAID);
        payment.setPayChannel(payChannel);
        payment.setThirdTradeNo(thirdTradeNo);
        payment.setUpdatedAt(now);
        orderPaymentRepository.updateStatus(payment);

        orderRepository.update(order);

        log.info("onPayFailed: state checked via state machine, tenantId={}, orderId={}, fromStatus={}, toStatus={}, channel={}, tradeNo={}",
                tenantId, orderId, currentStatus, nextStatus, payChannel, thirdTradeNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onPayTimeoutCancel(Long tenantId, Long orderId) {
        if (tenantId == null || orderId == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "tenantId/orderId 不能为空");
        }
        Order order = orderRepository.findById(tenantId, orderId);
        if (order == null) {
            log.warn("onPayTimeoutCancel: order not found, tenantId={}, orderId={}", tenantId, orderId);
            return;
        }
        OrderStatus currentStatus = order.getStatus();
        if (currentStatus != OrderStatus.PENDING_PAYMENT) {
            log.info("onPayTimeoutCancel: skip, currentStatus={}, tenantId={}, orderId={}", currentStatus, tenantId, orderId);
            return;
        }
        BizType bizType = order.getBizType();
        OrderStatus nextStatus = orderStateMachine.transitOrThrow(bizType, currentStatus, OrderEvent.AUTO_CANCEL_TIMEOUT);
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(nextStatus);
        order.setPayStatus(PayStatus.UNPAID);
        order.setUpdatedAt(now);
        orderRepository.update(order);

        OrderPayment payment = orderPaymentRepository.findByOrderId(tenantId, orderId);
        if (payment != null) {
            payment.setPayStatus(PayStatus.UNPAID);
            payment.setUpdatedAt(now);
            orderPaymentRepository.updateStatus(payment);
        }

        log.info("onPayTimeoutCancel: state changed via state machine, tenantId={}, orderId={}, fromStatus={}, toStatus={}",
                tenantId, orderId, currentStatus, nextStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onFullRefundSuccess(Long tenantId, Long orderId, BigDecimal refundAmount, String refundTradeNo) {
        if (tenantId == null || orderId == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "tenantId/orderId 不能为空");
        }
        Order order = orderRepository.findById(tenantId, orderId);
        if (order == null) {
            log.warn("onFullRefundSuccess: order not found, tenantId={}, orderId={}, refundTradeNo={}",
                    tenantId, orderId, refundTradeNo);
            return;
        }
        OrderPayment payment = orderPaymentRepository.findByOrderId(tenantId, orderId);
        if (payment == null) {
            log.error("onFullRefundSuccess: payment not found, tenantId={}, orderId={}, refundTradeNo={}",
                    tenantId, orderId, refundTradeNo);
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单支付记录不存在");
        }
        if (payment.getPayStatus() == PayStatus.REFUNDED) {
            log.info("onFullRefundSuccess: already refunded, tenantId={}, orderId={}, refundTradeNo={}",
                    tenantId, orderId, refundTradeNo);
            return;
        }

        BigDecimal payableAmount = order.getPayableAmount();
        if (refundAmount != null && payableAmount != null && refundAmount.compareTo(payableAmount) != 0) {
            log.warn("onFullRefundSuccess: refundAmount mismatch, tenantId={}, orderId={}, payableAmount={}, refundAmount={}",
                    tenantId, orderId, payableAmount, refundAmount);
        }

        OrderStatus currentStatus = order.getStatus();
        BizType bizType = order.getBizType();
        OrderStatus nextStatus = orderStateMachine.transitOrThrow(bizType, currentStatus, OrderEvent.FULL_REFUND);
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(nextStatus);
        order.setPayStatus(PayStatus.REFUNDED);
        order.setUpdatedAt(now);

        payment.setPayStatus(PayStatus.REFUNDED);
        payment.setUpdatedAt(now);
        Map<String, Object> extra = new HashMap<>();
        if (payment.getExtra() != null) {
            extra.putAll(payment.getExtra());
        }
        extra.put("refundType", "FULL");
        if (refundAmount != null) {
            extra.put("refundAmount", refundAmount);
        }
        if (refundTradeNo != null) {
            extra.put("refundTradeNo", refundTradeNo);
        }
        payment.setExtra(extra);
        orderPaymentRepository.updateStatus(payment);

        orderRepository.update(order);

        log.info("onFullRefundSuccess: state changed via state machine, tenantId={}, orderId={}, fromStatus={}, toStatus={}, refundAmount={}, refundTradeNo={}",
                tenantId, orderId, currentStatus, nextStatus, refundAmount, refundTradeNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onPartialRefundSuccess(Long tenantId, Long orderId, BigDecimal refundAmount, String refundTradeNo) {
        if (tenantId == null || orderId == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "tenantId/orderId 不能为空");
        }
        Order order = orderRepository.findById(tenantId, orderId);
        if (order == null) {
            log.warn("onPartialRefundSuccess: order not found, tenantId={}, orderId={}, refundTradeNo={}",
                    tenantId, orderId, refundTradeNo);
            return;
        }
        OrderPayment payment = orderPaymentRepository.findByOrderId(tenantId, orderId);
        if (payment == null) {
            log.error("onPartialRefundSuccess: payment not found, tenantId={}, orderId={}, refundTradeNo={}",
                    tenantId, orderId, refundTradeNo);
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单支付记录不存在");
        }
        if (payment.getPayStatus() == PayStatus.REFUNDED) {
            log.info("onPartialRefundSuccess: already fully refunded, tenantId={}, orderId={}, refundTradeNo={}",
                    tenantId, orderId, refundTradeNo);
            return;
        }

        OrderStatus currentStatus = order.getStatus();
        BizType bizType = order.getBizType();
        OrderStatus nextStatus = orderStateMachine.transitOrThrow(bizType, currentStatus, OrderEvent.PARTIAL_REFUND);
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(nextStatus);
        order.setPayStatus(PayStatus.REFUNDING);
        order.setUpdatedAt(now);

        payment.setPayStatus(PayStatus.REFUNDING);
        payment.setUpdatedAt(now);
        Map<String, Object> extra = new HashMap<>();
        if (payment.getExtra() != null) {
            extra.putAll(payment.getExtra());
        }
        extra.put("refundType", "PARTIAL");
        if (refundAmount != null) {
            extra.put("refundAmount", refundAmount);
        }
        if (refundTradeNo != null) {
            extra.put("refundTradeNo", refundTradeNo);
        }
        payment.setExtra(extra);
        orderPaymentRepository.updateStatus(payment);

        orderRepository.update(order);

        log.info("onPartialRefundSuccess: state changed via state machine, tenantId={}, orderId={}, fromStatus={}, toStatus={}, refundAmount={}, refundTradeNo={}",
                tenantId, orderId, currentStatus, nextStatus, refundAmount, refundTradeNo);
    }
}
