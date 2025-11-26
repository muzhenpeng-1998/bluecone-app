package com.bluecone.app.handler.order;

import com.bluecone.app.core.api.ApiHandler;
import com.bluecone.app.core.api.ApiRequest;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.dto.order.v2.OrderDetailResponseV2;
import com.bluecone.app.dto.order.v2.OrderItemV2;
import com.bluecone.app.service.OrderService;
import com.bluecone.app.core.domain.Order;
import com.bluecone.app.core.domain.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * 订单详情 API - V2 版本
 *
 * 新增字段：status, items（订单明细列表）
 */
@Component("Order.Detail.v2")
@RequiredArgsConstructor
public class OrderDetailV2Handler implements ApiHandler {

    private final OrderService orderService;

    @Override
    public Object handle(ApiRequest request) throws Exception {
        // 1. 获取参数
        String orderIdStr = request.getQueryParam("orderId");
        if (orderIdStr == null) {
            throw new BusinessException(ErrorCode.PARAM_MISSING.getCode(), "orderId is required");
        }

        Long orderId = Long.parseLong(orderIdStr);

        // 2. 调用 Service
        Order order = orderService.findById(orderId);

        // 3. 转换为 V2 DTO（包含更多字段）
        return OrderDetailResponseV2.builder()
            .orderId(order.getId())
            .amount(order.getAmount())
            .status(order.getStatus().name())
            .items(order.getItems().stream()
                .map(this::toItemV2)
                .collect(Collectors.toList()))
            .build();
    }

    private OrderItemV2 toItemV2(OrderItem item) {
        return OrderItemV2.builder()
            .itemId(item.getId())
            .productName(item.getProductName())
            .quantity(item.getQuantity())
            .price(item.getPrice())
            .build();
    }
}
