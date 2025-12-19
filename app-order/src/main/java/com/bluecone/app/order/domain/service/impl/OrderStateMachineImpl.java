package com.bluecone.app.order.domain.service.impl;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
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
     * 
     * <p><b>状态收口 V1：</b>使用 Canonical 状态，避免重复语义。</p>
     * <ul>
     *   <li>使用 WAIT_PAY 替代 PENDING_PAYMENT</li>
     *   <li>使用 WAIT_ACCEPT 替代 PENDING_ACCEPT</li>
     *   <li>使用 CANCELED 替代 CANCELLED</li>
     *   <li>保留 DRAFT/LOCKED_FOR_CHECKOUT 草稿态配置（仅用于购物车流程）</li>
     * </ul>
     */
    private static final Map<OrderStatus, Map<OrderEvent, OrderStatus>> TRANSITIONS;

    static {
        Map<OrderStatus, Map<OrderEvent, OrderStatus>> map = new EnumMap<>(OrderStatus.class);

        registerDraftTransitions(map);
        registerLockedTransitions(map);
        registerWaitPayTransitions(map);
        registerWaitAcceptTransitions(map);
        registerCompletedTransitions(map);
        registerCanceledTransitions(map);
        registerRefundedTransitions(map);
        
        // 兼容旧状态：将非 Canonical 状态映射到 Canonical 状态的转换规则
        registerLegacyTransitions(map);

        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    /**
     * 草稿态流转（仅购物车流程）。
     * <p>⚠️ DRAFT 不应写入订单主表，仅用于购物车草稿。</p>
     */
    private static void registerDraftTransitions(Map<OrderStatus, Map<OrderEvent, OrderStatus>> map) {
        EnumMap<OrderEvent, OrderStatus> draft = new EnumMap<>(OrderEvent.class);
        // 提交草稿 -> 使用 Canonical 状态 WAIT_PAY
        draft.put(OrderEvent.SUBMIT, OrderStatus.WAIT_PAY);
        // 用户取消 -> 使用 Canonical 状态 CANCELED
        draft.put(OrderEvent.USER_CANCEL, OrderStatus.CANCELED);
        map.put(OrderStatus.DRAFT, Collections.unmodifiableMap(draft));
    }

    /**
     * 草稿锁定态流转（仅结算流程）。
     * <p>⚠️ LOCKED_FOR_CHECKOUT 不应写入订单主表，仅用于结算锁定。</p>
     */
    private static void registerLockedTransitions(Map<OrderStatus, Map<OrderEvent, OrderStatus>> map) {
        EnumMap<OrderEvent, OrderStatus> locked = new EnumMap<>(OrderEvent.class);
        // 提交结算 -> 使用 Canonical 状态 WAIT_PAY
        locked.put(OrderEvent.SUBMIT, OrderStatus.WAIT_PAY);
        // 用户取消 -> 使用 Canonical 状态 CANCELED
        locked.put(OrderEvent.USER_CANCEL, OrderStatus.CANCELED);
        map.put(OrderStatus.LOCKED_FOR_CHECKOUT, Collections.unmodifiableMap(locked));
    }

    /**
     * 待支付状态流转（Canonical 状态）。
     * <p>使用 WAIT_PAY 替代 PENDING_PAYMENT。</p>
     */
    private static void registerWaitPayTransitions(Map<OrderStatus, Map<OrderEvent, OrderStatus>> map) {
        EnumMap<OrderEvent, OrderStatus> waitPay = new EnumMap<>(OrderEvent.class);
        // 支付成功 -> 使用 Canonical 状态 WAIT_ACCEPT
        waitPay.put(OrderEvent.PAY_SUCCESS, OrderStatus.WAIT_ACCEPT);
        // 用户取消 -> 使用 Canonical 状态 CANCELED
        waitPay.put(OrderEvent.USER_CANCEL, OrderStatus.CANCELED);
        // 超时取消 -> 使用 Canonical 状态 CANCELED
        waitPay.put(OrderEvent.AUTO_CANCEL_TIMEOUT, OrderStatus.CANCELED);
        // 支付失败 -> 保持 WAIT_PAY（允许重试）
        waitPay.put(OrderEvent.PAY_FAILED, OrderStatus.WAIT_PAY);
        map.put(OrderStatus.WAIT_PAY, Collections.unmodifiableMap(waitPay));
    }

    /**
     * 待接单状态流转（Canonical 状态）。
     * <p>使用 WAIT_ACCEPT 替代 PENDING_ACCEPT。</p>
     */
    private static void registerWaitAcceptTransitions(Map<OrderStatus, Map<OrderEvent, OrderStatus>> map) {
        EnumMap<OrderEvent, OrderStatus> waitAccept = new EnumMap<>(OrderEvent.class);
        // 商户接单 -> ACCEPTED
        waitAccept.put(OrderEvent.MERCHANT_ACCEPT, OrderStatus.ACCEPTED);
        // 直接完成（自动接单场景）-> COMPLETED
        waitAccept.put(OrderEvent.COMPLETE, OrderStatus.COMPLETED);
        // 用户取消 -> 使用 Canonical 状态 CANCELED
        waitAccept.put(OrderEvent.USER_CANCEL, OrderStatus.CANCELED);
        // 商户取消 -> 使用 Canonical 状态 CANCELED
        waitAccept.put(OrderEvent.MERCHANT_CANCEL, OrderStatus.CANCELED);
        map.put(OrderStatus.WAIT_ACCEPT, Collections.unmodifiableMap(waitAccept));
    }

    /**
     * 已完成状态流转（Canonical 状态）。
     */
    private static void registerCompletedTransitions(Map<OrderStatus, Map<OrderEvent, OrderStatus>> map) {
        EnumMap<OrderEvent, OrderStatus> completed = new EnumMap<>(OrderEvent.class);
        // 全额退款 -> REFUNDED
        completed.put(OrderEvent.FULL_REFUND, OrderStatus.REFUNDED);
        // 部分退款 -> 保持 COMPLETED
        completed.put(OrderEvent.PARTIAL_REFUND, OrderStatus.COMPLETED);
        map.put(OrderStatus.COMPLETED, Collections.unmodifiableMap(completed));
    }

    /**
     * 已取消状态流转（Canonical 状态）。
     * <p>使用 CANCELED 替代 CANCELLED。</p>
     */
    private static void registerCanceledTransitions(Map<OrderStatus, Map<OrderEvent, OrderStatus>> map) {
        EnumMap<OrderEvent, OrderStatus> canceled = new EnumMap<>(OrderEvent.class);
        // 全额退款 -> REFUNDED
        canceled.put(OrderEvent.FULL_REFUND, OrderStatus.REFUNDED);
        // 部分退款 -> 保持 CANCELED
        canceled.put(OrderEvent.PARTIAL_REFUND, OrderStatus.CANCELED);
        map.put(OrderStatus.CANCELED, Collections.unmodifiableMap(canceled));
    }

    /**
     * 已退款状态流转（Canonical 状态，终态）。
     */
    private static void registerRefundedTransitions(Map<OrderStatus, Map<OrderEvent, OrderStatus>> map) {
        map.put(OrderStatus.REFUNDED, Collections.emptyMap());
    }

    /**
     * 兼容旧状态：将非 Canonical 状态映射到 Canonical 状态的转换规则。
     * <p>保留仅为兼容旧数据，新代码不应使用。</p>
     */
    private static void registerLegacyTransitions(Map<OrderStatus, Map<OrderEvent, OrderStatus>> map) {
        // PENDING_PAYMENT 使用与 WAIT_PAY 相同的转换规则
        EnumMap<OrderEvent, OrderStatus> pendingPayment = new EnumMap<>(OrderEvent.class);
        pendingPayment.put(OrderEvent.PAY_SUCCESS, OrderStatus.WAIT_ACCEPT);
        pendingPayment.put(OrderEvent.USER_CANCEL, OrderStatus.CANCELED);
        pendingPayment.put(OrderEvent.AUTO_CANCEL_TIMEOUT, OrderStatus.CANCELED);
        pendingPayment.put(OrderEvent.PAY_FAILED, OrderStatus.WAIT_PAY);
        map.put(OrderStatus.PENDING_PAYMENT, Collections.unmodifiableMap(pendingPayment));
        
        // PENDING_ACCEPT 使用与 WAIT_ACCEPT 相同的转换规则
        EnumMap<OrderEvent, OrderStatus> pendingAccept = new EnumMap<>(OrderEvent.class);
        pendingAccept.put(OrderEvent.MERCHANT_ACCEPT, OrderStatus.ACCEPTED);
        pendingAccept.put(OrderEvent.COMPLETE, OrderStatus.COMPLETED);
        pendingAccept.put(OrderEvent.USER_CANCEL, OrderStatus.CANCELED);
        pendingAccept.put(OrderEvent.MERCHANT_CANCEL, OrderStatus.CANCELED);
        map.put(OrderStatus.PENDING_ACCEPT, Collections.unmodifiableMap(pendingAccept));
        
        // CANCELLED 使用与 CANCELED 相同的转换规则
        EnumMap<OrderEvent, OrderStatus> cancelled = new EnumMap<>(OrderEvent.class);
        cancelled.put(OrderEvent.FULL_REFUND, OrderStatus.REFUNDED);
        cancelled.put(OrderEvent.PARTIAL_REFUND, OrderStatus.CANCELED);
        map.put(OrderStatus.CANCELLED, Collections.unmodifiableMap(cancelled));
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
            throw new BusinessException(CommonErrorCode.BAD_REQUEST,
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
