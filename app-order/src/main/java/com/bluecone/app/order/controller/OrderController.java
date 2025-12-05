package com.bluecone.app.order.controller;

import com.bluecone.app.core.log.annotation.ApiLog;
import com.bluecone.app.order.service.OrderService;
import com.bluecone.app.order.service.ConfigDrivenOrderService;
import com.bluecone.app.order.application.command.ConfirmOrderCommand;
import com.bluecone.app.order.application.dto.ConfirmOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController("orderHelloController")
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ConfigDrivenOrderService configDrivenOrderService;

    @GetMapping("/hello")
    @ApiLog("测试事件日志")
    public Map<String, Object> hello() {
        return orderService.hello();
    }

    @PostMapping("/confirm")
    @ApiLog("确认订单")
    public com.bluecone.app.order.api.dto.ConfirmOrderResponse confirmOrder(@RequestBody ConfirmOrderRequest request) {
        ConfirmOrderCommand command = ConfirmOrderCommand.fromRequest(request, request.getTenantId(), request.getStoreId(), null);
        return configDrivenOrderService.confirmOrder(command);
    }
}
