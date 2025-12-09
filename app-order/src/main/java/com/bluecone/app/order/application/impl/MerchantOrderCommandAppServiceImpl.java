package com.bluecone.app.order.application.impl;

import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.api.dto.MerchantOrderView;
import com.bluecone.app.order.application.MerchantOrderCommandAppService;
import com.bluecone.app.order.application.command.MerchantAcceptOrderCommand;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.domain.event.OrderAcceptedEvent;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 商户操作订单的应用服务实现。
 */
@Service
@RequiredArgsConstructor
public class MerchantOrderCommandAppServiceImpl implements MerchantOrderCommandAppService {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * 商户接单：只允许 WAIT_ACCEPT 状态的订单被接单。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantOrderView acceptOrder(MerchantAcceptOrderCommand command) {
        if (command == null || command.getTenantId() == null || command.getOrderId() == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "租户/订单ID 不能为空");
        }
        Order order = orderRepository.findById(command.getTenantId(), command.getOrderId());
        if (order == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单不存在");
        }
        if (!Objects.equals(order.getStoreId(), command.getStoreId())) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单不属于当前门店");
        }
        boolean alreadyAccepted = OrderStatus.ACCEPTED.equals(order.getStatus());
        order.accept(command.getOperatorId());
        orderRepository.update(order);
        MerchantOrderView view = MerchantOrderView.from(order);
        if (!alreadyAccepted && OrderStatus.ACCEPTED.equals(order.getStatus())) {
            OrderAcceptedEvent event = new OrderAcceptedEvent(
                    command.getTenantId(),
                    command.getStoreId(),
                    order.getId(),
                    command.getOperatorId(),
                    extractPayOrderId(order),
                    toCents(order.getPayableAmount())
            );
            eventPublisher.publish(event);
        }
        return view;
    }

    private Long extractPayOrderId(Order order) {
        if (order == null) {
            return null;
        }
        Map<String, Object> ext = order.getExt();
        if (ext == null) {
            return null;
        }
        Object raw = ext.get("payOrderId");
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        if (raw instanceof String) {
            try {
                return Long.parseLong((String) raw);
            } catch (NumberFormatException ignored) {
                // ignore parse failure
            }
        }
        return null;
    }

    private Long toCents(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }
}
