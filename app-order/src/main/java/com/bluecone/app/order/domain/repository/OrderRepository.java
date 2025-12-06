package com.bluecone.app.order.domain.repository;

import com.bluecone.app.order.domain.model.Order;
import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * 用户订单分页查询。
     */
    List<Order> findUserOrders(Long tenantId,
                               Long userId,
                               List<String> statusList,
                               LocalDateTime fromTime,
                               LocalDateTime toTime,
                               int offset,
                               int limit);

    /**
     * 用户订单计数。
     */
    long countUserOrders(Long tenantId,
                         Long userId,
                         List<String> statusList,
                         LocalDateTime fromTime,
                         LocalDateTime toTime);

    /**
     * 商户侧：按门店查询订单列表。
     */
    List<Order> findStoreOrders(Long tenantId,
                                Long storeId,
                                List<String> statusList,
                                String orderSource,
                                LocalDateTime fromTime,
                                LocalDateTime toTime,
                                int offset,
                                int limit);

    /**
     * 商户侧：按门店查询订单总数。
     */
    long countStoreOrders(Long tenantId,
                          Long storeId,
                          List<String> statusList,
                          String orderSource,
                          LocalDateTime fromTime,
                          LocalDateTime toTime);
}
