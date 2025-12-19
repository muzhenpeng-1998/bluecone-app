package com.bluecone.app.order.domain.service.impl;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.order.api.dto.ConfirmOrderItemDTO;
import com.bluecone.app.order.api.dto.ConfirmOrderRequest;
import com.bluecone.app.order.domain.enums.BizType;
import com.bluecone.app.order.domain.enums.OrderEvent;
import com.bluecone.app.order.domain.enums.OrderSource;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.domain.enums.PayStatus;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.model.OrderItem;
import com.bluecone.app.order.domain.service.OrderDomainService;
import com.bluecone.app.order.domain.service.OrderStateMachine;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class OrderDomainServiceImpl implements OrderDomainService {

    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");

    private final OrderStateMachine orderStateMachine;

    public OrderDomainServiceImpl(OrderStateMachine orderStateMachine) {
        this.orderStateMachine = orderStateMachine;
    }

    @Override
    public Order buildConfirmedOrder(ConfirmOrderRequest request) {
        if (request == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "请求不能为空");
        }
        validateBasic(request);

        BizType bizType = BizType.fromCode(request.getBizType());
        if (bizType == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "不支持的业务业态");
        }
        OrderSource orderSource = OrderSource.fromCode(request.getOrderSource());
        if (orderSource == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "不支持的订单场景");
        }

        LocalDateTime now = LocalDateTime.now();
        List<OrderItem> items = buildItems(request.getItems(), now);

        Order order = Order.builder()
                .tenantId(request.getTenantId())
                .storeId(request.getStoreId())
                .userId(request.getUserId())
                .sessionId(request.getSessionId())
                .sessionVersion(defaultIfNull(request.getSessionVersion(), 0))
                .orderNo(null)
                .clientOrderNo(request.getClientOrderNo())
                .bizType(bizType)
                .orderSource(orderSource)
                .channel(request.getChannel())
                .discountAmount(defaultBigDecimal(request.getClientDiscountAmount()))
                .currency("CNY")
                .status(OrderStatus.DRAFT)
                .payStatus(PayStatus.UNPAID)
                .remark(request.getRemark())
                .ext(defaultMap(request.getExt()))
                .items(items)
                .version(0)
                .createdAt(now)
                .createdBy(null)
                .updatedAt(now)
                .updatedBy(null)
                .build();

        order.recalculateAmounts();
        order.validateAgainstClientAmounts(request.getClientPayableAmount(), AMOUNT_TOLERANCE);

        OrderStatus fromStatus = OrderStatus.DRAFT;
        OrderStatus nextStatus = orderStateMachine.transitOrThrow(bizType, fromStatus, OrderEvent.SUBMIT);
        order.setStatus(nextStatus);
        order.setPayStatus(PayStatus.UNPAID);

        order.setUpdatedAt(LocalDateTime.now());
        return order;
    }

    private void validateBasic(ConfirmOrderRequest request) {
        if (request.getTenantId() == null || request.getStoreId() == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "租户或门店不能为空");
        }
        if (!StringUtils.hasText(request.getChannel())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "下单渠道不能为空");
        }
        if (!StringUtils.hasText(request.getClientOrderNo())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "客户端订单号不能为空");
        }
        if (CollectionUtils.isEmpty(request.getItems())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "订单明细不能为空");
        }
    }

    private List<OrderItem> buildItems(List<ConfirmOrderItemDTO> itemDTOs, LocalDateTime now) {
        List<OrderItem> items = new ArrayList<>();
        for (ConfirmOrderItemDTO dto : itemDTOs) {
            if (dto == null) {
                continue;
            }
            if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, "明细数量必须大于0");
            }
            OrderItem item = new OrderItem();
            item.setProductId(dto.getProductId());
            item.setSkuId(dto.getSkuId());
            item.setProductName(dto.getProductName());
            item.setSkuName(dto.getSkuName());
            item.setProductCode(dto.getProductCode());
            item.setQuantity(dto.getQuantity());
            item.setRemark(dto.getRemark());
            item.setAttrs(defaultMap(dto.getAttrs()));
            item.setUnitPrice(defaultBigDecimal(dto.getClientUnitPrice()));
            item.setDiscountAmount(BigDecimal.ZERO);
            item.setPayableAmount(BigDecimal.ZERO);
            item.setCreatedAt(now);
            item.setUpdatedAt(now);
            items.add(item);
        }
        return items;
    }

    private Map<String, Object> defaultMap(Map<String, Object> source) {
        return source == null ? Map.of() : source;
    }

    private Integer defaultIfNull(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
