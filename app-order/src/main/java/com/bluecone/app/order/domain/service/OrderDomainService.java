package com.bluecone.app.order.domain.service;

import com.bluecone.app.order.api.dto.ConfirmOrderRequest;
import com.bluecone.app.order.domain.model.Order;

/**
 * 订单领域服务：
 * 负责将确认订单请求转换为领域订单聚合，并在领域内完成金额计算与规则校验。
 */
public interface OrderDomainService {

    /**
     * 根据确认订单请求构建订单聚合（未落库）。
     */
    Order buildConfirmedOrder(ConfirmOrderRequest request);
}
