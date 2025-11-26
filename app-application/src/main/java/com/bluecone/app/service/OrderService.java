package com.bluecone.app.service;

import com.bluecone.app.core.domain.Order;
import com.bluecone.app.core.domain.OrderItem;
import com.bluecone.app.core.domain.OrderStatus;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.infra.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单领域服务
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;

    /**
     * 根据 ID 查询订单（含明细）
     */
    public Order findById(Long orderId) {
        // 实际应从数据库查询，此处简化为假数据
        if (orderId == null || orderId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "Invalid orderId");
        }

        // 模拟数据库查询
        return Order.builder()
            .id(orderId)
            .tenantId(1L)
            .amount(new BigDecimal("299.00"))
            .status(OrderStatus.PAID)
            .items(List.of(
                OrderItem.builder()
                    .id(1L)
                    .productName("商品A")
                    .quantity(2)
                    .price(new BigDecimal("99.50"))
                    .build(),
                OrderItem.builder()
                    .id(2L)
                    .productName("商品B")
                    .quantity(1)
                    .price(new BigDecimal("100.00"))
                    .build()
            ))
            .build();
    }
}
