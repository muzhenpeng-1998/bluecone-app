package com.bluecone.app.order.controller;

import com.bluecone.app.core.log.annotation.ApiLog;
import com.bluecone.app.core.user.domain.member.repository.read.PageResult;
import com.bluecone.app.order.api.ApiResponse;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.store.api.dto.StoreOrderSnapshot;
import com.bluecone.app.order.api.dto.ConfirmOrderPreviewRequest;
import com.bluecone.app.order.api.dto.ConfirmOrderPreviewResponse;
import com.bluecone.app.order.api.dto.ConfirmOrderRequest;
import com.bluecone.app.order.api.dto.ConfirmOrderResponse;
import com.bluecone.app.order.api.dto.MerchantAcceptOrderRequest;
import com.bluecone.app.order.api.dto.MerchantOrderDetailView;
import com.bluecone.app.order.api.dto.MerchantOrderListQuery;
import com.bluecone.app.order.api.dto.MerchantOrderSummaryView;
import com.bluecone.app.order.api.dto.MerchantOrderView;
import com.bluecone.app.order.api.dto.UserOrderDetailView;
import com.bluecone.app.order.api.dto.UserOrderListQuery;
import com.bluecone.app.order.api.dto.UserOrderRefundRequest;
import com.bluecone.app.order.api.dto.UserOrderSummaryView;
import com.bluecone.app.order.application.MerchantOrderCommandAppService;
import com.bluecone.app.order.application.MerchantOrderQueryAppService;
import com.bluecone.app.order.application.OrderConfirmAppService;
import com.bluecone.app.order.application.UserOrderCommandAppService;
import com.bluecone.app.order.application.UserOrderPreviewAppService;
import com.bluecone.app.order.application.UserOrderQueryAppService;
import com.bluecone.app.order.application.UserOrderRefundAppService;
import com.bluecone.app.order.application.command.ConfirmOrderCommand;
import com.bluecone.app.order.application.command.MerchantAcceptOrderCommand;
import com.bluecone.app.order.service.ConfigDrivenOrderService;
import com.bluecone.app.order.service.OrderService;
import com.bluecone.app.order.controller.support.RequestContextHelper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@RestController("orderHelloController")
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final ConfigDrivenOrderService configDrivenOrderService;
    private final OrderConfirmAppService orderConfirmAppService;
    private final UserOrderPreviewAppService userOrderPreviewAppService;
    private final UserOrderCommandAppService userOrderCommandAppService;
    private final UserOrderQueryAppService userOrderQueryAppService;
    private final UserOrderRefundAppService userOrderRefundAppService;
    private final MerchantOrderQueryAppService merchantOrderQueryAppService;
    private final MerchantOrderCommandAppService merchantOrderCommandAppService;

    @GetMapping("/hello")
    @ApiLog("测试事件日志")
    public Map<String, Object> hello() {
        return orderService.hello();
    }

    @PostMapping("/confirm")
    @ApiLog("确认订单")
    public ConfirmOrderResponse confirmOrder(@RequestBody com.bluecone.app.order.api.dto.ConfirmOrderRequest request) {
        ConfirmOrderCommand command = ConfirmOrderCommand.fromRequest(request, request.getTenantId(), request.getStoreId(), null);
        return configDrivenOrderService.confirmOrder(command);
    }

    /**
     * 小程序用户提交订单。
     */
    @PostMapping("/user/orders/submit")
    @ApiLog("用户提交订单")
    public ApiResponse<ConfirmOrderResponse> submitUserOrder(
            @jakarta.validation.Valid @RequestBody ConfirmOrderRequest request) {
        Long tenantId = RequestContextHelper.currentTenantId();
        Long storeId = RequestContextHelper.currentStoreId();
        Long userId = RequestContextHelper.currentUserId();
        if (tenantId == null || storeId == null || userId == null) {
            throw BusinessException.of(ErrorCode.PARAM_MISSING.getCode(), "tenantId/userId/storeId missing");
        }
        request.setTenantId(tenantId);
        request.setStoreId(storeId);
        request.setUserId(userId);
        StoreOrderSnapshot snapshot = RequestContextHelper.currentStoreSnapshot();
        if (snapshot == null || !Boolean.TRUE.equals(snapshot.getCanAcceptOrder())) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "门店已打烊或暂停接单");
        }
        log.info("User submit order, tenantId={}, storeId={}, userId={}, clientOrderNo={}, items={}",
                tenantId,
                storeId,
                userId,
                request.getClientOrderNo(),
                request.getItems() != null ? request.getItems().size() : 0);
        ConfirmOrderResponse response = orderConfirmAppService.confirmOrder(request);
        return ApiResponse.success(response);
    }

    /**
     * 小程序用户确认页预览。
     */
    @PostMapping("/user/orders/preview")
    @ApiLog("用户确认订单预览")
    public ApiResponse<ConfirmOrderPreviewResponse> previewUserOrder(
            @jakarta.validation.Valid @RequestBody ConfirmOrderPreviewRequest request) {
        Long tenantId = RequestContextHelper.currentTenantId();
        Long storeId = RequestContextHelper.currentStoreId();
        Long userId = RequestContextHelper.currentUserId();
        if (tenantId == null || storeId == null || userId == null) {
            throw BusinessException.of(ErrorCode.PARAM_MISSING.getCode(), "tenantId/userId/storeId missing");
        }
        request.setTenantId(tenantId);
        request.setStoreId(storeId);
        request.setUserId(userId);
        StoreOrderSnapshot snapshot = RequestContextHelper.currentStoreSnapshot();
        if (snapshot == null || !Boolean.TRUE.equals(snapshot.getCanAcceptOrder())) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "门店已打烊或暂停接单");
        }
        log.info("User preview order, tenantId={}, storeId={}, userId={}, items={}",
                tenantId,
                storeId,
                userId,
                request.getItems() != null ? request.getItems().size() : 0);
        ConfirmOrderPreviewResponse resp = userOrderPreviewAppService.preview(request);
        return ApiResponse.success(resp);
    }

    /**
     * 小程序用户取消订单。
     */
    @PostMapping("/user/orders/{orderId}/cancel")
    @ApiLog("用户取消订单")
    public ApiResponse<Void> cancelUserOrder(
            @org.springframework.web.bind.annotation.PathVariable("orderId") Long orderId,
            @org.springframework.web.bind.annotation.RequestParam(value = "tenantId", required = false) Long tenantId,
            @org.springframework.web.bind.annotation.RequestParam(value = "userId", required = false) Long userId) {
        // TODO: tenantId/userId 应从登录态上下文注入，当前阶段允许从请求参数读取。
        log.info("User cancel order request, tenantId={}, userId={}, orderId={}",
                tenantId, userId, orderId);
        userOrderCommandAppService.cancelOrder(tenantId, userId, orderId);
        return ApiResponse.success();
    }

    /**
     * 小程序用户删除订单（软删除）。
     */
    @org.springframework.web.bind.annotation.DeleteMapping("/user/orders/{orderId}")
    @ApiLog("用户删除订单")
    public ApiResponse<Void> deleteUserOrder(
            @org.springframework.web.bind.annotation.PathVariable("orderId") Long orderId,
            @org.springframework.web.bind.annotation.RequestParam(value = "tenantId", required = false) Long tenantId,
            @org.springframework.web.bind.annotation.RequestParam(value = "userId", required = false) Long userId) {
        // TODO: tenantId/userId 应从登录态上下文注入，当前阶段允许从请求参数读取。
        log.info("User delete order request, tenantId={}, userId={}, orderId={}",
                tenantId, userId, orderId);
        userOrderCommandAppService.deleteOrder(tenantId, userId, orderId);
        return ApiResponse.success();
    }

    /**
     * 小程序用户再来一单（返回预览结果）。
     */
    @PostMapping("/user/orders/{orderId}/reorder")
    @ApiLog("用户再来一单预览")
    public ApiResponse<ConfirmOrderPreviewResponse> reorderUserOrder(
            @org.springframework.web.bind.annotation.PathVariable("orderId") Long orderId,
            @org.springframework.web.bind.annotation.RequestParam(value = "tenantId", required = false) Long tenantId,
            @org.springframework.web.bind.annotation.RequestParam(value = "userId", required = false) Long userId) {
        // TODO: tenantId/userId 最终应从登录态上下文注入。
        log.info("User reorder request, tenantId={}, userId={}, orderId={}",
                tenantId, userId, orderId);
        ConfirmOrderPreviewResponse resp = userOrderQueryAppService.reorder(tenantId, userId, orderId);
        return ApiResponse.success(resp);
    }

    /**
     * 小程序用户申请退款。
     */
    @PostMapping("/user/orders/{orderId}/refunds")
    @ApiLog("用户申请退款")
    public ApiResponse<Void> applyOrderRefund(
            @PathVariable("orderId") Long orderId,
            @jakarta.validation.Valid @RequestBody UserOrderRefundRequest request) {
        // TODO: tenantId/userId 应从登录态上下文注入，当前阶段允许从请求体读取。
        log.info("User apply refund request, tenantId={}, userId={}, orderId={}, refundType={}, refundAmount={}",
                request.getTenantId(), request.getUserId(), orderId, request.getRefundType(), request.getRefundAmount());
        userOrderRefundAppService.applyRefund(orderId, request);
        return ApiResponse.success();
    }

    /**
     * 小程序用户订单列表。
     */
    @GetMapping("/user/orders")
    @ApiLog("用户订单列表")
    public ApiResponse<PageResult<UserOrderSummaryView>> listUserOrders(
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "fromTime", required = false) String fromTime,
            @RequestParam(value = "toTime", required = false) String toTime,
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        // TODO: tenantId/userId 最终应从登录态/上下文注入，当前阶段允许从请求参数读取。
        UserOrderListQuery query = new UserOrderListQuery();
        query.setTenantId(tenantId);
        query.setUserId(userId);
        query.setStatus(status);
        query.setFromTime(fromTime);
        query.setToTime(toTime);
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        log.info("User list orders, tenantId={}, userId={}, status={}, from={}, to={}, pageNo={}, pageSize={}",
                tenantId, userId, status, fromTime, toTime, pageNo, pageSize);
        PageResult<UserOrderSummaryView> result = userOrderQueryAppService.listUserOrders(query);
        return ApiResponse.success(result);
    }

    /**
     * 小程序用户订单详情。
     */
    @GetMapping("/user/orders/{orderId}")
    @ApiLog("用户订单详情")
    public ApiResponse<UserOrderDetailView> getUserOrderDetail(
            @PathVariable("orderId") Long orderId,
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam(value = "userId", required = false) Long userId) {
        // TODO: tenantId/userId 最终应从登录态/上下文注入。
        log.info("User get order detail, tenantId={}, userId={}, orderId={}",
                tenantId, userId, orderId);
        UserOrderDetailView view = userOrderQueryAppService.getUserOrderDetail(tenantId, userId, orderId);
        return ApiResponse.success(view);
    }

    /**
     * 商户侧订单列表。
     */
    @GetMapping("/merchant/orders")
    @ApiLog("商户订单列表")
    public ApiResponse<PageResult<MerchantOrderSummaryView>> listMerchantOrders(
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam(value = "operatorId", required = false) Long operatorId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "orderSource", required = false) String orderSource,
            @RequestParam(value = "fromTime", required = false) String fromTime,
            @RequestParam(value = "toTime", required = false) String toTime,
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        // TODO: tenantId/storeId/operatorId 按最终方案应从登录态/上下文注入。
        MerchantOrderListQuery query = new MerchantOrderListQuery();
        query.setTenantId(tenantId);
        query.setStoreId(storeId);
        query.setOperatorId(operatorId);
        query.setStatus(status);
        query.setOrderSource(orderSource);
        query.setFromTime(fromTime);
        query.setToTime(toTime);
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        log.info("Merchant list orders, tenantId={}, storeId={}, operatorId={}, status={}, orderSource={}, from={}, to={}, pageNo={}, pageSize={}",
                tenantId, storeId, operatorId, status, orderSource, fromTime, toTime, pageNo, pageSize);
        PageResult<MerchantOrderSummaryView> result = merchantOrderQueryAppService.listStoreOrders(query);
        return ApiResponse.success(result);
    }

    /**
     * 商户侧订单详情。
     */
    @GetMapping("/merchant/orders/{orderId}")
    @ApiLog("商户订单详情")
    public ApiResponse<MerchantOrderDetailView> getMerchantOrderDetail(
            @PathVariable("orderId") Long orderId,
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam(value = "operatorId", required = false) Long operatorId) {
        // TODO: tenantId/storeId/operatorId 最终应从登录态上下文注入。
        log.info("Merchant get order detail, tenantId={}, storeId={}, operatorId={}, orderId={}",
                tenantId, storeId, operatorId, orderId);
        MerchantOrderDetailView view = merchantOrderQueryAppService.getStoreOrderDetail(tenantId, storeId, operatorId, orderId);
        return ApiResponse.success(view);
    }

    /**
     * 商户接单接口。
     */
    @PostMapping("/merchant/orders/{orderId}/accept")
    @ApiLog("商户接单")
    public ApiResponse<MerchantOrderView> acceptMerchantOrder(
            @PathVariable("orderId") Long orderId,
            @jakarta.validation.Valid @RequestBody MerchantAcceptOrderRequest request) {
        // TODO: tenantId/storeId/operatorId 后续应从登录态上下文注入。
        log.info("Merchant accept order request, tenantId={}, storeId={}, operatorId={}, orderId={}",
                request.getTenantId(), request.getStoreId(), request.getOperatorId(), orderId);
        MerchantAcceptOrderCommand command = MerchantAcceptOrderCommand.fromRequest(request, orderId);
        MerchantOrderView view = merchantOrderCommandAppService.acceptOrder(command);
        return ApiResponse.success(view);
    }
}
