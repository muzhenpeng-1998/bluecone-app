package com.bluecone.app.order.api.impl;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.order.api.OrderPaymentFacade;
import com.bluecone.app.order.domain.error.OrderErrorCode;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 订单支付门面实现。
 * <p>对外暴露订单侧的支付相关能力，供支付模块调用。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPaymentFacadeImpl implements OrderPaymentFacade {

    private final OrderRepository orderRepository;

    /**
     * 标记订单为已支付。
     * <p>状态机约束：只允许从 WAIT_PAY 状态流转到 PAID 状态。</p>
     * <p>幂等性：如果订单已经是 PAID 状态，则直接返回成功，不抛异常（允许重复回调）。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markOrderPaid(Long tenantId, Long orderId, Long payOrderId, String payChannel, String payNo, LocalDateTime paidAt) {
        log.info("标记订单为已支付：tenantId={}, orderId={}, payOrderId={}, payChannel={}, payNo={}", 
                tenantId, orderId, payOrderId, payChannel, payNo);
        
        // 1. 查询订单
        Order order = orderRepository.findById(tenantId, orderId);
        if (order == null) {
            log.error("订单不存在：tenantId={}, orderId={}", tenantId, orderId);
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        
        // 2. 调用领域模型方法，推进状态（含状态机校验和幂等处理）
        order.markPaid(payOrderId, payChannel, payNo, paidAt);
        
        // 3. 持久化订单
        orderRepository.update(order);
        
        log.info("订单支付成功：tenantId={}, orderId={}, status={}", tenantId, orderId, order.getStatus());
    }

    /**
     * 标记订单为已取消（带关单原因）。
     * <p>用于超时关单、用户取消、商户取消等场景。</p>
     * <p>幂等性：如果订单已经是 CANCELLED 状态，则直接返回成功。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markOrderCancelled(Long tenantId, Long orderId, String closeReason) {
        log.info("标记订单为已取消：tenantId={}, orderId={}, closeReason={}", tenantId, orderId, closeReason);
        
        // 1. 查询订单
        Order order = orderRepository.findById(tenantId, orderId);
        if (order == null) {
            log.error("订单不存在：tenantId={}, orderId={}", tenantId, orderId);
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        
        // 2. 调用领域模型方法，推进状态（含状态机校验和幂等处理）
        order.markCancelledWithReason(closeReason);
        
        // 3. 持久化订单
        orderRepository.update(order);
        
        log.info("订单已取消：tenantId={}, orderId={}, status={}, closeReason={}", 
                tenantId, orderId, order.getStatus(), closeReason);
    }
}
