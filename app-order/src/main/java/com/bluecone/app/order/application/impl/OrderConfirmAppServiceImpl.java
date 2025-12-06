package com.bluecone.app.order.application.impl;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.api.dto.ConfirmOrderRequest;
import com.bluecone.app.order.api.dto.ConfirmOrderResponse;
import com.bluecone.app.order.application.CollaborativeOrderSessionAppService;
import com.bluecone.app.order.application.OrderConfirmAppService;
import com.bluecone.app.order.application.assembler.OrderAppAssembler;
import com.bluecone.app.order.application.generator.OrderIdGenerator;
import com.bluecone.app.order.application.generator.OrderNoGenerator;
import com.bluecone.app.order.domain.enums.OrderEvent;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.domain.enums.PayStatus;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.model.OrderPayment;
import com.bluecone.app.order.domain.model.OrderSession;
import com.bluecone.app.order.domain.service.OrderDomainService;
import com.bluecone.app.order.domain.service.OrderStateMachine;
import com.bluecone.app.order.domain.repository.OrderPaymentRepository;
import com.bluecone.app.order.domain.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class OrderConfirmAppServiceImpl implements OrderConfirmAppService {

    private final OrderDomainService orderDomainService;
    private final OrderRepository orderRepository;
    private final OrderPaymentRepository orderPaymentRepository;
    private final OrderIdGenerator orderIdGenerator;
    private final OrderNoGenerator orderNoGenerator;
    private final CollaborativeOrderSessionAppService sessionAppService;
    private final OrderStateMachine orderStateMachine;
    public OrderConfirmAppServiceImpl(OrderDomainService orderDomainService,
                                      OrderRepository orderRepository,
                                      OrderPaymentRepository orderPaymentRepository,
                                      OrderIdGenerator orderIdGenerator,
                                      OrderNoGenerator orderNoGenerator,
                                      CollaborativeOrderSessionAppService sessionAppService,
                                      OrderStateMachine orderStateMachine) {
        this.orderDomainService = orderDomainService;
        this.orderRepository = orderRepository;
        this.orderPaymentRepository = orderPaymentRepository;
        this.orderIdGenerator = orderIdGenerator;
        this.orderNoGenerator = orderNoGenerator;
        this.sessionAppService = sessionAppService;
        this.orderStateMachine = orderStateMachine;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConfirmOrderResponse confirmOrder(ConfirmOrderRequest request) {
        requireClientOrderNo(request);
        requireTenantId(request);

        Order existed = orderRepository.findByClientOrderNo(request.getTenantId(), request.getClientOrderNo());
        if (existed != null) {
            return OrderAppAssembler.toConfirmResponse(existed);
        }

        validateSession(request);

        Order order = orderDomainService.buildConfirmedOrder(request);

        long orderId = orderIdGenerator.nextId();
        order.setId(orderId);
        order.setOrderNo(orderNoGenerator.generate(order));
        order.setCreatedBy(request.getUserId());
        order.setUpdatedBy(request.getUserId());

        orderRepository.save(order);

        if (Boolean.TRUE.equals(request.getAutoCreatePayment())) {
            createInitialPayment(order, request);
        }

        if (request.getSessionId() != null) {
            sessionAppService.closeSessionAfterConfirm(request.getTenantId(), request.getSessionId());
        }

        OrderStatus fromStatus = OrderStatus.DRAFT;
        OrderStatus expectedStatus = orderStateMachine.transitOrThrow(order.getBizType(), fromStatus, OrderEvent.SUBMIT);
        if (!expectedStatus.equals(order.getStatus())) {
            log.warn("Order status differs from state machine result, tenantId={}, storeId={}, userId={}, clientOrderNo={}, actualStatus={}, expectedStatus={}",
                    request.getTenantId(), request.getStoreId(), request.getUserId(), request.getClientOrderNo(), order.getStatus(), expectedStatus);
        }
        log.info("Order submit via state machine, tenantId={}, storeId={}, userId={}, clientOrderNo={}, fromStatus={}, toStatus={}",
                request.getTenantId(), request.getStoreId(), request.getUserId(), request.getClientOrderNo(),
                fromStatus, order.getStatus());

        return OrderAppAssembler.toConfirmResponse(order);
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

    private void createInitialPayment(Order order, ConfirmOrderRequest request) {
        OrderPayment payment = OrderPayment.builder()
                .id(orderIdGenerator.nextId())
                .tenantId(order.getTenantId())
                .storeId(order.getStoreId())
                .orderId(order.getId())
                .payChannel(request.getPayChannel())
                .payStatus(PayStatus.INIT)
                .payAmount(order.getPayableAmount() == null ? BigDecimal.ZERO : order.getPayableAmount())
                .currency(order.getCurrency())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        orderPaymentRepository.save(payment);
    }

    private void validateSession(ConfirmOrderRequest request) {
        if (request.getSessionId() == null) {
            return;
        }
        OrderSession session = sessionAppService.getSession(request.getTenantId(), request.getSessionId());
        if (session == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "会话不存在或已失效");
        }
        Integer clientVersion = request.getSessionVersion();
        if (clientVersion != null && !clientVersion.equals(session.getVersion())) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "会话版本冲突，请刷新后重试");
        }
    }
}
