package com.bluecone.app.order.domain.model;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.domain.enums.BizType;
import com.bluecone.app.order.domain.enums.OrderSource;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.domain.enums.PayStatus;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    // 标识与多租户
    private Long id;

    private Long tenantId;

    private Long storeId;

    // 用户 & 会话信息
    private Long userId;

    private String sessionId;

    @Builder.Default
    private Integer sessionVersion = 0;

    private String orderNo;

    private String clientOrderNo;

    // 业务维度
    private BizType bizType;

    private OrderSource orderSource;

    private String channel;

    // 金额
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal payableAmount = BigDecimal.ZERO;

    @Builder.Default
    private String currency = "CNY";

    // 状态
    private OrderStatus status;

    private PayStatus payStatus;

    // 备注 & 扩展
    private String remark;

    @Builder.Default
    private Map<String, Object> ext = Collections.emptyMap();

    // 聚合内明细
    @Builder.Default
    private List<OrderItem> items = Collections.emptyList();

    // 版本和审计字段
    @Builder.Default
    private Integer version = 0;

    private LocalDateTime createdAt;

    private Long createdBy;

    private LocalDateTime updatedAt;

    private Long updatedBy;

    /**
     * 根据 items 重算 totalAmount、discountAmount、payableAmount 等金额字段。
     */
    public void recalculateAmounts() {
        BigDecimal total = BigDecimal.ZERO;
        if (items != null) {
            for (OrderItem item : items) {
                if (item == null) {
                    continue;
                }
                BigDecimal unitPrice = item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice();
                int qty = item.getQuantity() == null ? 0 : item.getQuantity();
                total = total.add(unitPrice.multiply(BigDecimal.valueOf(qty)));
            }
        }
        BigDecimal discount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        this.totalAmount = total;
        this.discountAmount = discount;
        this.payableAmount = total.subtract(discount);
    }

    /**
     * 将前端传来的应付金额和当前订单的 payableAmount 做比对。
     *
     * @param clientPayableAmount 前端计算的应付金额
     * @param tolerance           允许的浮动误差
     */
    public void validateAgainstClientAmounts(BigDecimal clientPayableAmount, BigDecimal tolerance) {
        if (clientPayableAmount == null) {
            return;
        }
        BigDecimal serverPayable = payableAmount == null ? BigDecimal.ZERO : payableAmount;
        BigDecimal allowedTolerance = tolerance == null ? BigDecimal.ZERO : tolerance.abs();
        BigDecimal diff = serverPayable.subtract(clientPayableAmount).abs();
        if (diff.compareTo(allowedTolerance) > 0) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单金额校验失败，请刷新后重试");
        }
    }

    /**
     * 将订单标记为待支付。
     */
    public void markPendingPayment() {
        this.status = OrderStatus.PENDING_PAYMENT;
        this.payStatus = PayStatus.UNPAID;
    }

    /**
     * 标记为已支付。
     */
    public void markPaid() {
        this.payStatus = PayStatus.PAID;
        if (status != OrderStatus.CANCELLED && status != OrderStatus.COMPLETED && status != OrderStatus.REFUNDED) {
            this.status = OrderStatus.PENDING_ACCEPT;
        }
    }

    /**
     * 标记为已取消。
     */
    public void markCancelled() {
        this.status = OrderStatus.CANCELLED;
        if (this.payStatus == null) {
            this.payStatus = PayStatus.UNPAID;
        }
    }

    /**
     * 标记为已完成。
     */
    public void markCompleted() {
        this.status = OrderStatus.COMPLETED;
        if (this.payStatus == null) {
            this.payStatus = PayStatus.PAID;
        }
    }
}
