package com.bluecone.app.order.application.impl;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.api.dto.ConfirmOrderRequest;
import com.bluecone.app.order.api.dto.ConfirmOrderResponse;
import com.bluecone.app.order.application.OrderConfirmAppService;
import com.bluecone.app.order.application.command.ConfirmOrderCommand;
import com.bluecone.app.order.application.generator.OrderIdGenerator;
import com.bluecone.app.order.application.generator.OrderNoGenerator;
import com.bluecone.app.order.application.service.OrderPricingService;
import com.bluecone.app.order.application.service.OrderPricingService.PricingResult;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.repository.OrderRepository;
import com.bluecone.app.payment.simple.application.PaymentCreateAppService;
import com.bluecone.app.payment.simple.application.dto.PaymentOrderDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class OrderConfirmAppServiceImpl implements OrderConfirmAppService {

    private final OrderRepository orderRepository;
    private final OrderIdGenerator orderIdGenerator;
    private final OrderNoGenerator orderNoGenerator;
    private final PaymentCreateAppService paymentCreateAppService;
    private final OrderPricingService orderPricingService;

    public OrderConfirmAppServiceImpl(OrderRepository orderRepository,
                                      OrderIdGenerator orderIdGenerator,
                                      OrderNoGenerator orderNoGenerator,
                                      PaymentCreateAppService paymentCreateAppService,
                                      OrderPricingService orderPricingService) {
        this.orderRepository = orderRepository;
        this.orderIdGenerator = orderIdGenerator;
        this.orderNoGenerator = orderNoGenerator;
        this.paymentCreateAppService = paymentCreateAppService;
        this.orderPricingService = orderPricingService;
    }

    /**
     * 用户下单入口：转换命令、生成订单聚合并落库。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConfirmOrderResponse confirmOrder(ConfirmOrderRequest request) {
        requireClientOrderNo(request);
        requireTenantId(request);

        Order existed = orderRepository.findByClientOrderNo(request.getTenantId(), request.getClientOrderNo());
        if (existed != null) {
            log.info("clientOrderNo={} 已存在，直接返回已保存订单, orderId={}", request.getClientOrderNo(), existed.getId());
            return toConfirmResponse(existed, request.getPayChannel());
        }

        ConfirmOrderCommand command = ConfirmOrderCommand.fromRequest(
                request,
                request.getTenantId(),
                request.getStoreId(),
                request.getUserId());
        PricingResult pricing = orderPricingService.priceItems(request.getTenantId(), request.getStoreId(), request.getItems());
        BigDecimal totalAmount = OrderPricingService.toDecimal(pricing.getTotalAmountCents());
        request.setClientTotalAmount(totalAmount);
        request.setClientPayableAmount(totalAmount);
        request.setClientDiscountAmount(BigDecimal.ZERO);
        Order order = Order.createFromConfirmCommand(command);
        long orderId = orderIdGenerator.nextId();
        order.setId(orderId);
        order.setOrderNo(orderNoGenerator.generate(order));
        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setCreatedBy(request.getUserId());
        order.setUpdatedBy(request.getUserId());
        order.markCreated();

        orderRepository.save(order);

        log.info("小程序用户订单入库成功，tenantId={}, storeId={}, userId={}, orderId={}",
                order.getTenantId(), order.getStoreId(), order.getUserId(), order.getId());

        PaymentOrderDTO paymentOrder = paymentCreateAppService.createForOrder(
                order.getTenantId(),
                order.getStoreId(),
                order.getUserId(),
                order.getId(),
                toCents(order.getPayableAmount()));

        ConfirmOrderResponse response = toConfirmResponse(order, request.getPayChannel());
        if (paymentOrder != null) {
            response.setPayOrderId(paymentOrder.getId());
            response.setPaymentStatus(paymentOrder.getStatus());
        }
        return response;
    }

    /**
     * 构建确认结果响应。
     */
    private ConfirmOrderResponse toConfirmResponse(Order order, String payChannel) {
        ConfirmOrderResponse response = new ConfirmOrderResponse();
        response.setOrderId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setStatus(order.getStatus() != null ? order.getStatus().getCode() : null);
        response.setPayStatus(order.getPayStatus() != null ? order.getPayStatus().getCode() : null);
        response.setPayableAmount(order.getPayableAmount());
        response.setCurrency(order.getCurrency());
        response.setPayChannel(payChannel);
        response.setNeedPay(Boolean.TRUE);
        response.setPaymentTimeoutSeconds(0);
        return response;
    }

    private long toCents(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }

    private void requireClientOrderNo(ConfirmOrderRequest request) {
        if (!StringUtils.hasText(request.getClientOrderNo())) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "clientOrderNo 不能为空");
        }
    }

    private void requireTenantId(ConfirmOrderRequest request) {
        if (request.getTenantId() == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "tenantId 不能为空");
        }
    }
}
