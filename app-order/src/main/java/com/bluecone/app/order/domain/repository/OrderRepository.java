package com.bluecone.app.order.domain.repository;

import com.bluecone.app.order.domain.model.Order;

public interface OrderRepository {

    /**
     * 根据租户和订单ID查询订单聚合（包含明细），不存在返回 null。
     */
    Order findById(Long tenantId, Long orderId);

    /**
     * 根据租户和 clientOrderNo 查询订单，用于确认接口幂等。
     */
    Order findByClientOrderNo(Long tenantId, String clientOrderNo);

    /**
     * 新建订单（包含明细）。
     */
    void save(Order order);

    /**
     * 更新订单。
     */
    void update(Order order);
}
