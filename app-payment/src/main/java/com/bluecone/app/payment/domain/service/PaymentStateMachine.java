package com.bluecone.app.payment.domain.service;

import com.bluecone.app.payment.domain.enums.PaymentEvent;
import com.bluecone.app.payment.domain.enums.PaymentStatus;
import java.util.Set;

/**
 * 支付领域有限状态机：
 * - 负责“能否从当前状态接收事件”以及“事件后的目标状态”计算；
 * - 不做持久化，仅提供合法性与状态推导；资金与业务解耦，订单由订单域处理。
 */
public interface PaymentStateMachine {

    /**
     * 当前状态是否允许接收指定事件。
     *
     * @param bizTypeCode 业务业态编码（例如 COFFEE/HOT_POT），当前版本仅作扩展点
     * @param current     当前支付状态
     * @param event       即将发生的支付事件
     * @return true 表示可流转，false 表示非法
     */
    boolean canTransit(String bizTypeCode, PaymentStatus current, PaymentEvent event);

    /**
     * 执行状态流转并返回新状态。
     *
     * @throws com.bluecone.app.core.exception.BizException 当状态流转不合法时抛出。
     */
    PaymentStatus transitOrThrow(String bizTypeCode, PaymentStatus current, PaymentEvent event);

    /**
     * 返回当前状态下允许的所有事件，便于风控/前端展示“下一步可做什么”。
     */
    Set<PaymentEvent> nextEvents(String bizTypeCode, PaymentStatus current);
}
