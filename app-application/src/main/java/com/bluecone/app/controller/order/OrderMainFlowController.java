package com.bluecone.app.controller.order;

import com.bluecone.app.order.api.dto.OrderConfirmRequest;
import com.bluecone.app.order.api.dto.OrderConfirmResponse;
import com.bluecone.app.order.api.dto.OrderSubmitRequest;
import com.bluecone.app.order.api.dto.OrderSubmitResponse;
import com.bluecone.app.order.application.OrderConfirmApplicationService;
import com.bluecone.app.order.application.OrderSubmitApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订单主链路 Controller（M0）。
 * <p>提供订单确认单和提交单接口，遵循项目约定：Controller 仅做装配，业务编排在 app-order 的 application 层。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderMainFlowController {

    private final OrderConfirmApplicationService orderConfirmApplicationService;
    private final OrderSubmitApplicationService orderSubmitApplicationService;

    /**
     * 订单确认单接口（M0）。
     * <p>用户侧调用，用于预校验门店可接单、商品有效性、计算价格等，返回 confirmToken 供后续提交使用。</p>
     * <p>路径：POST /api/order/m0/confirm</p>
     *
     * @param request 确认单请求
     * @return 确认单响应（包含价格、门店可接单状态、confirmToken等）
     */
    @PostMapping("/m0/confirm")
    public OrderConfirmResponse confirm(@RequestBody OrderConfirmRequest request) {
        log.info("订单确认单请求：tenantId={}, storeId={}, userId={}, itemCount={}",
                request.getTenantId(), request.getStoreId(), request.getUserId(),
                request.getItems() != null ? request.getItems().size() : 0);

        OrderConfirmResponse response = orderConfirmApplicationService.confirm(request);

        log.info("订单确认单响应：tenantId={}, storeId={}, userId={}, storeAcceptable={}, payableAmount={}",
                request.getTenantId(), request.getStoreId(), request.getUserId(),
                response.getStoreAcceptable(), response.getPayableAmount());

        return response;
    }

    /**
     * 订单提交单接口（M0）。
     * <p>用户侧调用，用于正式创建订单并落库，必须携带 confirmToken 和 clientRequestId（幂等键）。</p>
     * <p>路径：POST /api/order/m0/submit</p>
     *
     * @param request 提交单请求
     * @return 提交单响应（包含订单ID、publicOrderNo、状态等）
     */
    @PostMapping("/m0/submit")
    public OrderSubmitResponse submit(@RequestBody OrderSubmitRequest request) {
        log.info("订单提交单请求：tenantId={}, storeId={}, userId={}, clientRequestId={}, itemCount={}",
                request.getTenantId(), request.getStoreId(), request.getUserId(),
                request.getClientRequestId(),
                request.getItems() != null ? request.getItems().size() : 0);

        OrderSubmitResponse response = orderSubmitApplicationService.submit(request);

        log.info("订单提交单响应：tenantId={}, storeId={}, userId={}, clientRequestId={}, orderId={}, publicOrderNo={}, idempotent={}",
                request.getTenantId(), request.getStoreId(), request.getUserId(),
                request.getClientRequestId(), response.getOrderId(), response.getPublicOrderNo(),
                response.getIdempotent());

        return response;
    }
}
