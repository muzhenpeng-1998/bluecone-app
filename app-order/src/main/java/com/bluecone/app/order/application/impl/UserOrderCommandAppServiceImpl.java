package com.bluecone.app.order.application.impl;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.application.UserOrderCommandAppService;
import com.bluecone.app.order.domain.enums.OrderEvent;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.domain.enums.PayStatus;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.repository.OrderRepository;
import com.bluecone.app.order.domain.service.OrderStateMachine;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserOrderCommandAppServiceImpl implements UserOrderCommandAppService {

    private final OrderRepository orderRepository;
    private final OrderStateMachine orderStateMachine;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long tenantId, Long userId, Long orderId) {
        if (tenantId == null || userId == null || orderId == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "tenantId/userId/orderId 不能为空");
        }
        Order order = orderRepository.findById(tenantId, orderId);
        if (order == null) {
            log.warn("User cancel order but order not found, tenantId={}, userId={}, orderId={}", tenantId, userId, orderId);
            return;
        }
        if (!userId.equals(order.getUserId())) {
            log.warn("User cancel order but not owner, tenantId={}, userId={}, orderUserId={}, orderId={}",
                    tenantId, userId, order.getUserId(), orderId);
            throw new BizException(CommonErrorCode.FORBIDDEN, "无权操作该订单");
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED) {
            log.info("User cancel order but already cancelled/refunded, tenantId={}, userId={}, orderId={}, status={}", tenantId, userId, orderId, order.getStatus());
            return;
        }
        if (!order.canCancelByUser()) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "当前状态不允许取消订单");
        }
        OrderStatus fromStatus = order.getStatus();
        OrderStatus toStatus = orderStateMachine.transitOrThrow(order.getBizType(), fromStatus, OrderEvent.USER_CANCEL);
        order.setStatus(toStatus);
        if (order.getPayStatus() == null) {
            order.setPayStatus(PayStatus.UNPAID);
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.update(order);
        log.info("User cancelled order via state machine, tenantId={}, userId={}, orderId={}, fromStatus={}, toStatus={}",
                tenantId, userId, orderId, fromStatus, toStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long tenantId, Long userId, Long orderId) {
        if (tenantId == null || userId == null || orderId == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "tenantId/userId/orderId 不能为空");
        }
        Order order = orderRepository.findById(tenantId, orderId);
        if (order == null) {
            log.warn("User delete order but not found, tenantId={}, userId={}, orderId={}", tenantId, userId, orderId);
            return;
        }
        if (!userId.equals(order.getUserId())) {
            log.warn("User delete order but not owner, tenantId={}, userId={}, orderUserId={}, orderId={}",
                    tenantId, userId, order.getUserId(), orderId);
            throw new BizException(CommonErrorCode.FORBIDDEN, "无权操作该订单");
        }
        if (order.isUserDeleted()) {
            log.info("User delete order but already deleted, tenantId={}, userId={}, orderId={}", tenantId, userId, orderId);
            return;
        }
        order.markUserDeleted();
        orderRepository.update(order);
        log.info("User deleted order (soft), tenantId={}, userId={}, orderId={}", tenantId, userId, orderId);
    }
}
