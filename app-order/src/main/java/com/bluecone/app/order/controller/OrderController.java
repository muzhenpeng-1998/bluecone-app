package com.bluecone.app.order.controller;

import com.bluecone.app.core.log.annotation.ApiLog;
import com.bluecone.app.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController("orderHelloController")
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/hello")
    @ApiLog("测试事件日志")
    public Map<String, Object> hello() {
        return orderService.hello();
    }
}
