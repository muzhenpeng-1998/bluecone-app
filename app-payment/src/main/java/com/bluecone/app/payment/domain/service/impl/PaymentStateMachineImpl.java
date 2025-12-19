package com.bluecone.app.payment.domain.service.impl;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.payment.domain.enums.PaymentEvent;
import com.bluecone.app.payment.domain.enums.PaymentStatus;
import com.bluecone.app.payment.domain.service.PaymentStateMachine;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 支付状态机默认实现：
 * - 基于状态 + 事件的有限状态机，提供合法性校验与目标状态计算；
 * - 预留 bizTypeCode 扩展点，当前版本未区分业态；
 * - 不做持久化，仅在领域层判断“能不能转”以及“转去哪”。
 */
@Service
public class PaymentStateMachineImpl implements PaymentStateMachine {

    /**
     * 状态流转表：key = 当前状态，value = (事件 -> 目标状态) 的不可变映射。
     */
    private static final Map<PaymentStatus, Map<PaymentEvent, PaymentStatus>> TRANSITIONS;

    static {
        Map<PaymentStatus, Map<PaymentEvent, PaymentStatus>> map = new EnumMap<>(PaymentStatus.class);

        registerInitTransitions(map);
        registerPendingTransitions(map);
        registerSuccessTransitions(map);
        registerFailedTransitions(map);
        registerCanceledTransitions(map);
        registerRefundingTransitions(map);
        registerRefundedTransitions(map);
        registerClosedTransitions(map);

        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    /**
     * INIT：首次创建的状态。
     * - INITIATE -> PENDING：向渠道发起预下单后进入待支付。
     * - USER/MERCHANT_CANCEL -> CANCELED：用户或商户主动取消。
     * - CLOSE -> CLOSED：风控/对账直接关闭资金单。
     */
    private static void registerInitTransitions(Map<PaymentStatus, Map<PaymentEvent, PaymentStatus>> map) {
        EnumMap<PaymentEvent, PaymentStatus> transitions = new EnumMap<>(PaymentEvent.class);
        transitions.put(PaymentEvent.INITIATE, PaymentStatus.PENDING);
        transitions.put(PaymentEvent.USER_CANCEL, PaymentStatus.CANCELED);
        transitions.put(PaymentEvent.MERCHANT_CANCEL, PaymentStatus.CANCELED);
        transitions.put(PaymentEvent.CLOSE, PaymentStatus.CLOSED);
        map.put(PaymentStatus.INIT, Collections.unmodifiableMap(transitions));
    }

    /**
     * PENDING：渠道已下单，等待用户支付。
     * - PAY_SUCCESS -> SUCCESS：渠道异步通知成功。
     * - PAY_FAILED -> FAILED：渠道失败/关单。
     * - USER_CANCEL / MERCHANT_CANCEL -> CANCELED：主动取消。
     * - PAY_TIMEOUT -> CANCELED：超时未支付。
     * - CLOSE -> CLOSED：风控/对账直接关闭。
     */
    private static void registerPendingTransitions(Map<PaymentStatus, Map<PaymentEvent, PaymentStatus>> map) {
        EnumMap<PaymentEvent, PaymentStatus> transitions = new EnumMap<>(PaymentEvent.class);
        transitions.put(PaymentEvent.PAY_SUCCESS, PaymentStatus.SUCCESS);
        transitions.put(PaymentEvent.PAY_FAILED, PaymentStatus.FAILED);
        transitions.put(PaymentEvent.USER_CANCEL, PaymentStatus.CANCELED);
        transitions.put(PaymentEvent.MERCHANT_CANCEL, PaymentStatus.CANCELED);
        transitions.put(PaymentEvent.PAY_TIMEOUT, PaymentStatus.CANCELED);
        transitions.put(PaymentEvent.CLOSE, PaymentStatus.CLOSED);
        map.put(PaymentStatus.PENDING, Collections.unmodifiableMap(transitions));
    }

    /**
     * SUCCESS：支付完成。
     * - APPLY_REFUND -> REFUNDING：发起退款流程。
     * - CLOSE -> CLOSED：风控/对账直接终止。
     */
    private static void registerSuccessTransitions(Map<PaymentStatus, Map<PaymentEvent, PaymentStatus>> map) {
        EnumMap<PaymentEvent, PaymentStatus> transitions = new EnumMap<>(PaymentEvent.class);
        transitions.put(PaymentEvent.APPLY_REFUND, PaymentStatus.REFUNDING);
        transitions.put(PaymentEvent.CLOSE, PaymentStatus.CLOSED);
        map.put(PaymentStatus.SUCCESS, Collections.unmodifiableMap(transitions));
    }

    /**
     * FAILED：支付失败，可重试或直接关闭。
     * - INITIATE -> PENDING：重新发起支付（兼容渠道关单后重新下单）。
     * - CLOSE -> CLOSED：归档关闭。
     */
    private static void registerFailedTransitions(Map<PaymentStatus, Map<PaymentEvent, PaymentStatus>> map) {
        EnumMap<PaymentEvent, PaymentStatus> transitions = new EnumMap<>(PaymentEvent.class);
        transitions.put(PaymentEvent.INITIATE, PaymentStatus.PENDING);
        transitions.put(PaymentEvent.CLOSE, PaymentStatus.CLOSED);
        map.put(PaymentStatus.FAILED, Collections.unmodifiableMap(transitions));
    }

    /**
     * CANCELED：已取消（未支付成功）。
     * - CLOSE -> CLOSED：归档关闭。
     */
    private static void registerCanceledTransitions(Map<PaymentStatus, Map<PaymentEvent, PaymentStatus>> map) {
        EnumMap<PaymentEvent, PaymentStatus> transitions = new EnumMap<>(PaymentEvent.class);
        transitions.put(PaymentEvent.CLOSE, PaymentStatus.CLOSED);
        map.put(PaymentStatus.CANCELED, Collections.unmodifiableMap(transitions));
    }

    /**
     * REFUNDING：退款处理中。
     * - REFUND_SUCCESS -> REFUNDED：退款完成。
     * - REFUND_FAILED -> SUCCESS：退款失败，回到成功状态以便重试。
     * - CLOSE -> CLOSED：风控/对账强制关闭。
     */
    private static void registerRefundingTransitions(Map<PaymentStatus, Map<PaymentEvent, PaymentStatus>> map) {
        EnumMap<PaymentEvent, PaymentStatus> transitions = new EnumMap<>(PaymentEvent.class);
        transitions.put(PaymentEvent.REFUND_SUCCESS, PaymentStatus.REFUNDED);
        transitions.put(PaymentEvent.REFUND_FAILED, PaymentStatus.SUCCESS);
        transitions.put(PaymentEvent.CLOSE, PaymentStatus.CLOSED);
        map.put(PaymentStatus.REFUNDING, Collections.unmodifiableMap(transitions));
    }

    /**
     * REFUNDED：退款完成。
     * - CLOSE -> CLOSED：归档关闭。
     */
    private static void registerRefundedTransitions(Map<PaymentStatus, Map<PaymentEvent, PaymentStatus>> map) {
        EnumMap<PaymentEvent, PaymentStatus> transitions = new EnumMap<>(PaymentEvent.class);
        transitions.put(PaymentEvent.CLOSE, PaymentStatus.CLOSED);
        map.put(PaymentStatus.REFUNDED, Collections.unmodifiableMap(transitions));
    }

    /**
     * CLOSED：终止态，不再接受任何事件。
     */
    private static void registerClosedTransitions(Map<PaymentStatus, Map<PaymentEvent, PaymentStatus>> map) {
        map.put(PaymentStatus.CLOSED, Collections.emptyMap());
    }

    @Override
    public boolean canTransit(String bizTypeCode, PaymentStatus current, PaymentEvent event) {
        if (current == null || event == null) {
            return false;
        }
        Map<PaymentEvent, PaymentStatus> eventMap = TRANSITIONS.get(current);
        return eventMap != null && eventMap.containsKey(event);
    }

    @Override
    public PaymentStatus transitOrThrow(String bizTypeCode, PaymentStatus current, PaymentEvent event) {
        if (!canTransit(bizTypeCode, current, event)) {
            String currentName = current == null ? "null" : current.name();
            String eventName = event == null ? "null" : event.name();
            throw new BusinessException(
                    CommonErrorCode.BAD_REQUEST,
                    String.format("非法的支付状态流转：current=%s, event=%s, bizType=%s", currentName, eventName, bizTypeCode)
            );
        }
        Map<PaymentEvent, PaymentStatus> eventMap = TRANSITIONS.get(current);
        return eventMap.get(event);
    }

    @Override
    public Set<PaymentEvent> nextEvents(String bizTypeCode, PaymentStatus current) {
        if (current == null) {
            return Set.of();
        }
        Map<PaymentEvent, PaymentStatus> eventMap = TRANSITIONS.get(current);
        if (eventMap == null || eventMap.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(eventMap.keySet());
    }
}
