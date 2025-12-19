package com.bluecone.app.order.application.impl;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.order.api.dto.ConfirmOrderRequest;
import com.bluecone.app.order.api.dto.ConfirmOrderResponse;
import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.order.application.OrderConfirmAppService;
import com.bluecone.app.order.application.command.ConfirmOrderCommand;
import com.bluecone.app.order.domain.event.OrderSubmittedEvent;
import com.bluecone.app.order.application.generator.OrderIdGenerator;
import com.bluecone.app.order.application.generator.OrderNoGenerator;
import com.bluecone.app.order.application.service.OrderPricingService;
import com.bluecone.app.order.application.service.OrderPricingService.PricingResult;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.repository.OrderRepository;
import com.bluecone.app.payment.simple.application.PaymentCreateAppService;
import com.bluecone.app.payment.simple.application.dto.PaymentOrderDTO;
import com.bluecone.app.order.application.service.WalletPaymentService;
import com.bluecone.app.wallet.api.dto.WalletAssetCommand;
import com.bluecone.app.wallet.api.dto.WalletAssetResult;
import com.bluecone.app.wallet.api.facade.WalletAssetFacade;
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
    private final DomainEventPublisher eventPublisher;
    private final WalletAssetFacade walletAssetFacade;
    private final WalletPaymentService walletPaymentService;

    public OrderConfirmAppServiceImpl(OrderRepository orderRepository,
                                      OrderIdGenerator orderIdGenerator,
                                      OrderNoGenerator orderNoGenerator,
                                      PaymentCreateAppService paymentCreateAppService,
                                      OrderPricingService orderPricingService,
                                      DomainEventPublisher eventPublisher,
                                      WalletAssetFacade walletAssetFacade,
                                      WalletPaymentService walletPaymentService) {
        this.orderRepository = orderRepository;
        this.orderIdGenerator = orderIdGenerator;
        this.orderNoGenerator = orderNoGenerator;
        this.paymentCreateAppService = paymentCreateAppService;
        this.orderPricingService = orderPricingService;
        this.eventPublisher = eventPublisher;
        this.walletAssetFacade = walletAssetFacade;
        this.walletPaymentService = walletPaymentService;
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

        // M5：如果使用钱包余额支付，冻结余额并立即完成支付
        PaymentOrderDTO paymentOrder = null;
        if (Boolean.TRUE.equals(request.getUseWalletBalance())) {
            // 1. 冻结余额
            freezeWalletBalance(order, request.getUserId());
            
            // 2. 立即完成钱包支付（提交冻结并标记订单已支付）
            boolean paySuccess = walletPaymentService.payWithWallet(
                    order.getTenantId(), 
                    request.getUserId(), 
                    order.getId()
            );
            
            if (!paySuccess) {
                log.error("钱包支付失败：orderId={}, userId={}", order.getId(), request.getUserId());
                throw new BusinessException(CommonErrorCode.SYSTEM_ERROR, "钱包支付失败");
            }
            
            log.info("钱包余额支付完成：orderId={}, userId={}, amount={}", 
                    order.getId(), request.getUserId(), order.getPayableAmount());
        } else {
            // 只有非钱包支付才创建支付单
            paymentOrder = paymentCreateAppService.createForOrder(
                    order.getTenantId(),
                    order.getStoreId(),
                    order.getUserId(),
                    order.getId(),
                    toCents(order.getPayableAmount()));
        }

        ConfirmOrderResponse response = toConfirmResponse(order, request.getPayChannel());
        if (paymentOrder != null) {
            response.setPayOrderId(paymentOrder.getId());
            response.setPaymentStatus(paymentOrder.getStatus());
        }
        long totalAmountCents = toCents(order.getPayableAmount());
        OrderSubmittedEvent submittedEvent = new OrderSubmittedEvent(
                order.getTenantId(),
                order.getStoreId(),
                order.getUserId(),
                order.getId(),
                paymentOrder != null ? paymentOrder.getId() : null,
                totalAmountCents,
                order.getChannel() != null ? order.getChannel() : request.getChannel());
        eventPublisher.publish(submittedEvent);
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
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "clientOrderNo 不能为空");
        }
    }

    private void requireTenantId(ConfirmOrderRequest request) {
        if (request.getTenantId() == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "tenantId 不能为空");
        }
    }
    
    /**
     * 冻结钱包余额（M5 新增）
     * 当用户选择钱包余额支付时，在下单时冻结对应金额
     */
    private void freezeWalletBalance(Order order, Long userId) {
        try {
            // 构造幂等键：{tenantId}:{userId}:{orderId}:freeze
            String idempotencyKey = String.format("%d:%d:%d:freeze", 
                    order.getTenantId(), userId, order.getId());
            
            WalletAssetCommand freezeCommand = WalletAssetCommand.builder()
                    .tenantId(order.getTenantId())
                    .userId(userId)
                    .amount(order.getPayableAmount())
                    .bizType("ORDER_CHECKOUT")
                    .bizOrderId(order.getId())
                    .bizOrderNo(order.getOrderNo())
                    .idempotencyKey(idempotencyKey)
                    .operatorId(userId)
                    .remark("订单下单冻结余额")
                    .build();
            
            WalletAssetResult result = walletAssetFacade.freeze(freezeCommand);
            if (!result.isSuccess()) {
                log.error("冻结钱包余额失败：orderId={}, userId={}, amount={}, error={}", 
                        order.getId(), userId, order.getPayableAmount(), result.getErrorMessage());
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, 
                        "冻结钱包余额失败：" + result.getErrorMessage());
            }
            
            log.info("冻结钱包余额成功：orderId={}, userId={}, amount={}, freezeNo={}, idempotent={}", 
                    order.getId(), userId, order.getPayableAmount(), 
                    result.getFreezeNo(), result.isIdempotent());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("冻结钱包余额异常：orderId={}, userId={}, amount={}", 
                    order.getId(), userId, order.getPayableAmount(), e);
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR, "冻结钱包余额失败");
        }
    }
}
