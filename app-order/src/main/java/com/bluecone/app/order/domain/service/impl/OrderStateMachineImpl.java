package com.bluecone.app.order.domain.service.impl;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.domain.enums.BizType;
import com.bluecone.app.order.domain.enums.OrderEvent;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.domain.service.OrderStateMachine;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class OrderStateMachineImpl implements OrderStateMachine {

    /**
     * 状态流转表：
     * key: 当前状态
     * value: (事件 -> 目标状态) 映射
     */
    private static final Map<OrderStatus, Map<OrderEvent, OrderStatus>> TRANSITIONS;

    static {
        Map<OrderStatus, Map<OrderEvent, OrderStatus>> map = new EnumMap<>(OrderStatus.class);

        registerDraftTransitions(map);
        registerPendingPaymentTransitions(map);
        registerPendingAcceptTransitions(map);
        registerCompletedTransitions(map);
        registerCancelledTransitions(map);
        registerRefundedTransitions(map);

        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    private static void registerDraftTransitions(Map<OrderStatus, Map<OrderEvent, OrderStatus>> map) {
        EnumMap<OrderEvent, OrderStatus> draft = new EnumMap<>(OrderEvent.class);
        draft.put(OrderEvent.SUBMIT, OrderStatus.PENDING_PAYMENT);
        draft.put(OrderEvent.USER_CANCEL, OrderStatus.CANCELLED);
        map.put(OrderStatus.DRAFT, Collections.unmodifiableMap(draft));
    }

    private static void registerPendingPaymentTransitions(Map<OrderStatus, Map<OrderEvent, OrderStatus>> map) {
        EnumMap<OrderEvent, OrderStatus> pendingPay = new EnumMap<>(OrderEvent.class);
        pendingPay.put(OrderEvent.PAY_SUCCESS, OrderStatus.PENDING_ACCEPT);
        pendingPay.put(OrderEvent.USER_CANCEL, OrderStatus.CANCELLED);
        pendingPay.put(OrderEvent.AUTO_CANCEL_TIMEOUT, OrderStatus.CANCELLED);
        pendingPay.put(OrderEvent.PAY_FAILED, OrderStatus.PENDING_PAYMENT);
        map.put(OrderStatus.PENDING_PAYMENT, Collections.unmodifiableMap(pendingPay));
    }

    private static void registerPendingAcceptTransitions(Map<OrderStatus, Map<OrderEvent, OrderStatus>> map) {
        EnumMap<OrderEvent, OrderStatus> pendingAccept = new EnumMap<>(OrderEvent.class);
        pendingAccept.put(OrderEvent.COMPLETE, OrderStatus.COMPLETED);
        pendingAccept.put(OrderEvent.USER_CANCEL, OrderStatus.CANCELLED);
        pendingAccept.put(OrderEvent.MERCHANT_CANCEL, OrderStatus.CANCELLED);
        map.put(OrderStatus.PENDING_ACCEPT, Collections.unmodifiableMap(pendingAccept));
    }

    private static void registerCompletedTransitions(Map<OrderStatus, Map<OrderEvent, OrderStatus>> map) {
        EnumMap<OrderEvent, OrderStatus> completed = new EnumMap<>(OrderEvent.class);
        completed.put(OrderEvent.FULL_REFUND, OrderStatus.REFUNDED);
        completed.put(OrderEvent.PARTIAL_REFUND, OrderStatus.COMPLETED);
        map.put(OrderStatus.COMPLETED, Collections.unmodifiableMap(completed));
    }

    private static void registerCancelledTransitions(Map<OrderStatus, Map<OrderEvent, OrderStatus>> map) {
        EnumMap<OrderEvent, OrderStatus> cancelled = new EnumMap<>(OrderEvent.class);
        cancelled.put(OrderEvent.FULL_REFUND, OrderStatus.REFUNDED);
        cancelled.put(OrderEvent.PARTIAL_REFUND, OrderStatus.CANCELLED);
        map.put(OrderStatus.CANCELLED, Collections.unmodifiableMap(cancelled));
    }

    private static void registerRefundedTransitions(Map<OrderStatus, Map<OrderEvent, OrderStatus>> map) {
        map.put(OrderStatus.REFUNDED, Collections.emptyMap());
    }

    @Override
    public boolean canTransit(BizType bizType, OrderStatus current, OrderEvent event) {
        if (current == null || event == null) {
            return false;
        }
        Map<OrderEvent, OrderStatus> eventMap = TRANSITIONS.get(current);
        return eventMap != null && eventMap.containsKey(event);
    }

    @Override
    public OrderStatus transitOrThrow(BizType bizType, OrderStatus current, OrderEvent event) {
        if (!canTransit(bizType, current, event)) {
            throw new BizException(CommonErrorCode.BAD_REQUEST,
                    String.format("非法的订单状态流转：current=%s, event=%s",
                            current != null ? current.name() : "null",
                            event != null ? event.name() : "null"));
        }
        Map<OrderEvent, OrderStatus> eventMap = TRANSITIONS.get(current);
        return eventMap.get(event);
    }

    @Override
    public Set<OrderEvent> nextEvents(BizType bizType, OrderStatus current) {
        if (current == null) {
            return Set.of();
        }
        Map<OrderEvent, OrderStatus> eventMap = TRANSITIONS.get(current);
        if (eventMap == null || eventMap.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(eventMap.keySet());
    }
}
