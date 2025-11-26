package com.bluecone.app.core.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单领域模型（不与 DTO 混用）
 */
public class Order {

    /** 订单 ID */
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 订单金额 */
    private BigDecimal amount;

    /** 订单状态 */
    private OrderStatus status;

    /** 订单明细列表 */
    private List<OrderItem> items;

    public Order() {
    }

    public Order(Long id, Long tenantId, BigDecimal amount, OrderStatus status, List<OrderItem> items) {
        this.id = id;
        this.tenantId = tenantId;
        this.amount = amount;
        this.status = status;
        this.items = items;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private BigDecimal amount;
        private OrderStatus status;
        private List<OrderItem> items;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder tenantId(Long tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder status(OrderStatus status) {
            this.status = status;
            return this;
        }

        public Builder items(List<OrderItem> items) {
            this.items = items;
            return this;
        }

        public Order build() {
            return new Order(id, tenantId, amount, status, items);
        }
    }
}
