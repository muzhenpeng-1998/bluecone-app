package com.bluecone.app.order.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.model.OrderItem;
import com.bluecone.app.order.domain.repository.OrderRepository;
import com.bluecone.app.order.infra.persistence.converter.OrderConverter;
import com.bluecone.app.order.infra.persistence.mapper.OrderItemMapper;
import com.bluecone.app.order.infra.persistence.mapper.OrderMapper;
import com.bluecone.app.order.infra.persistence.po.OrderItemPO;
import com.bluecone.app.order.infra.persistence.po.OrderPO;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @Override
    public Order findById(Long tenantId, Long orderId) {
        OrderPO orderPO = orderMapper.selectOne(new LambdaQueryWrapper<OrderPO>()
                .eq(OrderPO::getTenantId, tenantId)
                .eq(OrderPO::getId, orderId));
        if (orderPO == null) {
            return null;
        }
        List<OrderItemPO> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemPO>()
                .eq(OrderItemPO::getTenantId, tenantId)
                .eq(OrderItemPO::getOrderId, orderId));
        return OrderConverter.toDomain(orderPO, items);
    }

    @Override
    public Order findByClientOrderNo(Long tenantId, String clientOrderNo) {
        OrderPO orderPO = orderMapper.selectOne(new LambdaQueryWrapper<OrderPO>()
                .eq(OrderPO::getTenantId, tenantId)
                .eq(OrderPO::getClientOrderNo, clientOrderNo));
        if (orderPO == null) {
            return null;
        }
        List<OrderItemPO> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemPO>()
                .eq(OrderItemPO::getTenantId, tenantId)
                .eq(OrderItemPO::getOrderId, orderPO.getId()));
        return OrderConverter.toDomain(orderPO, items);
    }

    @Override
    public void save(Order order) {
        if (order == null) {
            return;
        }
        OrderPO orderPO = OrderConverter.toPO(order);
        orderMapper.insert(orderPO);
        List<OrderItemPO> itemPOS = order.getItems() == null ? List.of() : order.getItems()
                .stream()
                .filter(Objects::nonNull)
                .map(item -> OrderConverter.toItemPO(order, item))
                .collect(Collectors.toList());
        for (OrderItemPO itemPO : itemPOS) {
            orderItemMapper.insert(itemPO);
        }
    }

    @Override
    public void update(Order order) {
        if (order == null || order.getId() == null || order.getTenantId() == null) {
            return;
        }
        Integer oldVersion = order.getVersion();
        OrderPO po = OrderConverter.toPO(order);
        po.setVersion(oldVersion == null ? 1 : oldVersion + 1);
        int updated = orderMapper.update(po, new LambdaUpdateWrapper<OrderPO>()
                .eq(OrderPO::getId, order.getId())
                .eq(OrderPO::getTenantId, order.getTenantId())
                .eq(oldVersion != null, OrderPO::getVersion, oldVersion));
        if (updated == 0) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单版本冲突，请刷新后重试");
        }
        order.setVersion(po.getVersion());
    }

    @Override
    public List<Order> findUserOrders(Long tenantId,
                                      Long userId,
                                      List<String> statusList,
                                      java.time.LocalDateTime fromTime,
                                      java.time.LocalDateTime toTime,
                                      int offset,
                                      int limit) {
        LambdaQueryWrapper<OrderPO> wrapper = baseUserWrapper(tenantId, userId, statusList, fromTime, toTime);
        wrapper.orderByDesc(OrderPO::getCreatedAt);
        wrapper.last("LIMIT " + offset + "," + limit);
        List<OrderPO> poList = orderMapper.selectList(wrapper);
        if (poList == null || poList.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = poList.stream().map(OrderPO::getId).collect(Collectors.toList());
        List<OrderItemPO> itemList = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemPO>()
                .eq(OrderItemPO::getTenantId, tenantId)
                .in(OrderItemPO::getOrderId, orderIds));
        var itemsByOrder = itemList.stream().collect(Collectors.groupingBy(OrderItemPO::getOrderId));
        return poList.stream()
                .map(po -> OrderConverter.toDomain(po, itemsByOrder.getOrDefault(po.getId(), List.of())))
                .collect(Collectors.toList());
    }

    @Override
    public long countUserOrders(Long tenantId,
                                Long userId,
                                List<String> statusList,
                                java.time.LocalDateTime fromTime,
                                java.time.LocalDateTime toTime) {
        LambdaQueryWrapper<OrderPO> wrapper = baseUserWrapper(tenantId, userId, statusList, fromTime, toTime);
        return orderMapper.selectCount(wrapper);
    }

    @Override
    public List<Order> findStoreOrders(Long tenantId,
                                       Long storeId,
                                       List<String> statusList,
                                       String orderSource,
                                       java.time.LocalDateTime fromTime,
                                       java.time.LocalDateTime toTime,
                                       int offset,
                                       int limit) {
        LambdaQueryWrapper<OrderPO> wrapper = storeWrapper(tenantId, storeId, statusList, orderSource, fromTime, toTime);
        wrapper.orderByDesc(OrderPO::getCreatedAt);
        wrapper.last("LIMIT " + offset + "," + limit);
        List<OrderPO> poList = orderMapper.selectList(wrapper);
        if (poList == null || poList.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = poList.stream().map(OrderPO::getId).collect(Collectors.toList());
        List<OrderItemPO> itemList = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemPO>()
                .eq(OrderItemPO::getTenantId, tenantId)
                .in(OrderItemPO::getOrderId, orderIds));
        var itemsByOrder = itemList.stream().collect(Collectors.groupingBy(OrderItemPO::getOrderId));
        return poList.stream()
                .map(po -> OrderConverter.toDomain(po, itemsByOrder.getOrDefault(po.getId(), List.of())))
                .collect(Collectors.toList());
    }

    @Override
    public long countStoreOrders(Long tenantId,
                                 Long storeId,
                                 List<String> statusList,
                                 String orderSource,
                                 java.time.LocalDateTime fromTime,
                                 java.time.LocalDateTime toTime) {
        LambdaQueryWrapper<OrderPO> wrapper = storeWrapper(tenantId, storeId, statusList, orderSource, fromTime, toTime);
        return orderMapper.selectCount(wrapper);
    }

    private LambdaQueryWrapper<OrderPO> baseUserWrapper(Long tenantId,
                                                        Long userId,
                                                        List<String> statusList,
                                                        java.time.LocalDateTime fromTime,
                                                        java.time.LocalDateTime toTime) {
        LambdaQueryWrapper<OrderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderPO::getTenantId, tenantId)
                .eq(OrderPO::getUserId, userId)
                .and(w -> w.isNull(OrderPO::getUserDeleted).or().eq(OrderPO::getUserDeleted, false));
        if (statusList != null && !statusList.isEmpty()) {
            wrapper.in(OrderPO::getStatus, statusList);
        }
        if (fromTime != null) {
            wrapper.ge(OrderPO::getCreatedAt, fromTime);
        }
        if (toTime != null) {
            wrapper.le(OrderPO::getCreatedAt, toTime);
        }
        return wrapper;
    }

    private LambdaQueryWrapper<OrderPO> storeWrapper(Long tenantId,
                                                     Long storeId,
                                                     List<String> statusList,
                                                     String orderSource,
                                                     java.time.LocalDateTime fromTime,
                                                     java.time.LocalDateTime toTime) {
        LambdaQueryWrapper<OrderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderPO::getTenantId, tenantId)
                .eq(OrderPO::getStoreId, storeId);
        if (statusList != null && !statusList.isEmpty()) {
            wrapper.in(OrderPO::getStatus, statusList);
        }
        if (orderSource != null && !orderSource.isBlank()) {
            wrapper.eq(OrderPO::getOrderSource, orderSource);
        }
        if (fromTime != null) {
            wrapper.ge(OrderPO::getCreatedAt, fromTime);
        }
        if (toTime != null) {
            wrapper.le(OrderPO::getCreatedAt, toTime);
        }
        return wrapper;
    }
}
