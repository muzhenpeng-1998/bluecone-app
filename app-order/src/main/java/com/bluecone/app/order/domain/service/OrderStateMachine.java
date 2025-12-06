package com.bluecone.app.order.domain.service;

import com.bluecone.app.order.domain.enums.BizType;
import com.bluecone.app.order.domain.enums.OrderEvent;
import com.bluecone.app.order.domain.enums.OrderStatus;
import java.util.Set;

/**
 * 订单状态机：
 * - 负责根据当前状态 + 事件，计算下一个状态；
 * - 若状态流转不合法，抛出业务异常；
 * - 预留按 BizType（业态）扩展不同状态图的能力。
 */
public interface OrderStateMachine {

    /**
     * 当前状态是否允许接收指定事件。
     */
    boolean canTransit(BizType bizType, OrderStatus current, OrderEvent event);

    /**
     * 计算状态流转结果。
     *
     * @throws com.bluecone.app.core.exception.BizException 当状态流转不合法时抛出。
     */
    OrderStatus transitOrThrow(BizType bizType, OrderStatus current, OrderEvent event);

    /**
     * 返回当前状态下允许的所有事件，便于风控/前端展示。
     */
    Set<OrderEvent> nextEvents(BizType bizType, OrderStatus current);
}
