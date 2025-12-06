package com.bluecone.app.order.event;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.model.OrderItem;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单创建后的领域事件，适用于 Outbox 异步投递。
 */
public class OrderCreatedEvent extends DomainEvent {

    private final Long orderId;
    private final Long tenantId;
    private final Long storeId;
    private final Long userId;
    private final String orderNo;
    private final String channel;
    private final String scene;
    private final BigDecimal totalAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal payableAmount;
    private final List<OrderItemSnapshot> items;

    public OrderCreatedEvent(@NotNull Order order, String scene, EventMetadata metadata) {
        super(OrderEventNames.ORDER_CREATED, metadata);
        this.orderId = order.getId();
        this.tenantId = order.getTenantId();
        this.storeId = order.getStoreId();
        this.userId = order.getUserId();
        this.orderNo = order.getOrderNo();
        this.channel = order.getChannel();
        this.scene = scene;
        this.totalAmount = order.getTotalAmount();
        this.discountAmount = order.getDiscountAmount();
        this.payableAmount = order.getPayableAmount();
        List<OrderItem> itemList = order.getItems();
        this.items = itemList == null ? Collections.emptyList() : itemList.stream()
                .map(OrderItemSnapshot::from)
                .collect(Collectors.toList());
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getChannel() {
        return channel;
    }

    public String getScene() {
        return scene;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getPayableAmount() {
        return payableAmount;
    }

    public List<OrderItemSnapshot> getItems() {
        return items;
    }

    public static class OrderItemSnapshot {
        private final Long skuId;
        private final Integer quantity;
        private final BigDecimal unitPrice;

        private OrderItemSnapshot(Long skuId, Integer quantity, BigDecimal unitPrice) {
            this.skuId = skuId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public static OrderItemSnapshot from(OrderItem item) {
            if (item == null) {
                return new OrderItemSnapshot(null, null, null);
            }
            return new OrderItemSnapshot(item.getSkuId(), item.getQuantity(), item.getUnitPrice());
        }

        public Long getSkuId() {
            return skuId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }
    }
}
