package com.bluecone.app.order.application.order;

import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.event.EventMetadata;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.order.api.order.OrderSubmitFacade;
import com.bluecone.app.order.api.order.dto.OrderSubmitResponse;
import com.bluecone.app.order.api.order.dto.SubmitOrderFromDraftDTO;
import com.bluecone.app.order.application.generator.OrderNoGenerator;
import com.bluecone.app.order.domain.command.SubmitOrderFromDraftCommand;
import com.bluecone.app.order.domain.enums.OrderSource;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.error.OrderErrorCode;
import com.bluecone.app.order.domain.repository.OrderDraftRepository;
import com.bluecone.app.order.domain.repository.OrderRepository;
import com.bluecone.app.order.event.OrderCreatedEvent;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 订单提交应用服务：从草稿转为正式订单 + Outbox 通知。
 */
@Service
public class OrderSubmitApplicationService implements OrderSubmitFacade {

    private static final String DEFAULT_CHANNEL = "WECHAT_MINI";
    private static final String DEFAULT_SCENE = OrderSource.DINE_IN.getCode();

    private final OrderDraftRepository orderDraftRepository;
    private final OrderRepository orderRepository;
    private final OrderNoGenerator orderNoGenerator;
    private final DomainEventPublisher domainEventPublisher;

    public OrderSubmitApplicationService(OrderDraftRepository orderDraftRepository,
                                         OrderRepository orderRepository,
                                         OrderNoGenerator orderNoGenerator,
                                         DomainEventPublisher domainEventPublisher) {
        this.orderDraftRepository = orderDraftRepository;
        this.orderRepository = orderRepository;
        this.orderNoGenerator = orderNoGenerator;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitResponse submitOrderFromCurrentDraft(SubmitOrderFromDraftDTO command) {
        validate(command);
        String channel = StringUtils.hasText(command.getChannel()) ? command.getChannel() : DEFAULT_CHANNEL;
        String scene = StringUtils.hasText(command.getScene()) ? command.getScene() : DEFAULT_SCENE;
        Order draft = orderDraftRepository.findDraft(
                        command.getTenantId(),
                        command.getStoreId(),
                        command.getUserId(),
                        channel,
                        scene)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND, "当前没有可提交的订单草稿"));
        if (!OrderStatus.DRAFT.equals(draft.getStatus()) && !OrderStatus.LOCKED_FOR_CHECKOUT.equals(draft.getStatus())) {
            throw new BusinessException(OrderErrorCode.ORDER_STATE_INVALID, "订单当前状态不可提交");
        }
        SubmitOrderFromDraftCommand domainCommand = toDomainCommand(command);
        draft.confirmFromDraft(domainCommand);
        if (StringUtils.hasText(scene)) {
            OrderSource source = OrderSource.fromCode(scene);
            if (source != null) {
                draft.setOrderSource(source);
            }
        }
        if (!StringUtils.hasText(draft.getChannel())) {
            draft.setChannel(channel);
        }
        if (!StringUtils.hasText(draft.getOrderNo())) {
            draft.setOrderNo(orderNoGenerator.generate(draft));
        }
        orderRepository.update(draft);
        domainEventPublisher.publish(new OrderCreatedEvent(draft, scene, EventMetadata.of(buildMetadata(command))));
        return toResponse(draft);
    }

    private SubmitOrderFromDraftCommand toDomainCommand(SubmitOrderFromDraftDTO command) {
        BigDecimal clientAmount = BigDecimal.valueOf(command.getClientPayableAmount()).divide(BigDecimal.valueOf(100));
        return SubmitOrderFromDraftCommand.builder()
                .orderToken(command.getOrderToken())
                .clientPayableAmount(clientAmount)
                .userRemark(command.getUserRemark())
                .contactName(command.getContactName())
                .contactPhone(command.getContactPhone())
                .addressJson(command.getAddressJson())
                .build();
    }

    private OrderSubmitResponse toResponse(Order order) {
        OrderSubmitResponse resp = new OrderSubmitResponse();
        resp.setOrderId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setStatus(order.getStatus() != null ? order.getStatus().getCode() : null);
        resp.setPayStatus(order.getPayStatus() != null ? order.getPayStatus().getCode() : null);
        resp.setPayableAmount(toCents(order.getPayableAmount()));
        resp.setCurrency(order.getCurrency());
        resp.setChannel(order.getChannel());
        resp.setScene(order.getOrderSource() != null ? order.getOrderSource().getCode() : null);
        return resp;
    }

    private Map<String, String> buildMetadata(SubmitOrderFromDraftDTO command) {
        return Map.of(
                "tenantId", String.valueOf(command.getTenantId()),
                "storeId", String.valueOf(command.getStoreId()),
                "userId", String.valueOf(command.getUserId()),
                "orderToken", String.valueOf(command.getOrderToken())
        );
    }

    private void validate(SubmitOrderFromDraftDTO command) {
        if (command == null) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID, "订单提交参数不能为空");
        }
        if (command.getTenantId() == null || command.getStoreId() == null || command.getUserId() == null) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID, "租户/门店/用户信息缺失");
        }
        if (!StringUtils.hasText(command.getOrderToken())) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID, "orderToken 不能为空");
        }
        if (command.getClientPayableAmount() == null || command.getClientPayableAmount() < 0) {
            throw new BusinessException(OrderErrorCode.ORDER_INVALID, "clientPayableAmount 缺失或非法");
        }
    }

    private long toCents(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }

}
