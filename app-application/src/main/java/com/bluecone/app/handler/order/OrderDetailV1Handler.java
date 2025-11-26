package com.bluecone.app.handler.order;

import com.bluecone.app.core.api.ApiHandler;
import com.bluecone.app.core.api.ApiRequest;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.dto.order.v1.OrderDetailResponseV1;
import com.bluecone.app.service.OrderService;
import com.bluecone.app.core.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 订单详情 API - V1 版本
 *
 * 返回字段：orderId, amount
 */
@Component("Order.Detail.v1")
@RequiredArgsConstructor
public class OrderDetailV1Handler implements ApiHandler {

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

        // 3. 转换为 V1 DTO
        return OrderDetailResponseV1.builder()
            .orderId(order.getId())
            .amount(order.getAmount())
            .build();
    }
}
