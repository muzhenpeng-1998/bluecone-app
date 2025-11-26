package com.bluecone.app.controller;

import com.bluecone.app.api.CommandRouter;
import com.bluecone.app.core.log.annotation.ApiLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单 API Controller（极简版本）
 *
 * 特点：
 * - 不关心版本逻辑，只负责路由入口
 * - 不处理异常，交由 GlobalExceptionHandler
 * - 不做业务逻辑，只调用 CommandRouter
 * - 永远不会因为新增版本而修改
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final CommandRouter commandRouter;

    /**
     * 查询订单详情（多版本支持）
     * 版本识别：
     * - Header: X-Api-Version: 2
     * - URL: /api/v2/order/detail
     * - Query: /api/order/detail?version=2
     */
    @GetMapping("/detail")
    @ApiLog("查询订单详情")
    public Object detail(HttpServletRequest request) throws Exception {
        return commandRouter.route("Order.Detail", request);
    }
}
