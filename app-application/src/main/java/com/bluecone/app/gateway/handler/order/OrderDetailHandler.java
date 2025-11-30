package com.bluecone.app.gateway.handler.order;

import com.bluecone.app.core.domain.Order;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.dto.order.v1.OrderDetailResponseV1;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.ApiHandler;
import com.bluecone.app.service.OrderService;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

/**
 * Order detail handler (v1).
 */
@Component
@RequiredArgsConstructor
public class OrderDetailHandler implements ApiHandler<Void, OrderDetailResponseV1> {

    private final OrderService orderService;

    @Override
    public OrderDetailResponseV1 handle(ApiContext ctx, Void request) {
        String orderIdRaw = ctx.getPathVariables().get("id");
        if (!StringUtils.hasText(orderIdRaw)) {
            throw BusinessException.of(ErrorCode.PARAM_MISSING.getCode(), "orderId is required");
        }
        Long orderId = Long.parseLong(orderIdRaw);
        Order order = orderService.findById(orderId);
        return OrderDetailResponseV1.builder()
                .orderId(order.getId())
                .amount(order.getAmount())
                .build();
    }
}
