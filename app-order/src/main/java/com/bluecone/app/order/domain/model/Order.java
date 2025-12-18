package com.bluecone.app.order.domain.model;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.application.command.ConfirmOrderCommand;
import com.bluecone.app.order.domain.command.SubmitOrderFromDraftCommand;
import com.bluecone.app.order.domain.enums.BizType;
import com.bluecone.app.order.domain.enums.OrderSource;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.domain.enums.PayStatus;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final BigDecimal CONFIRM_TOLERANCE = new BigDecimal("0.01");

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

    private Boolean userDeleted;

    private LocalDateTime userDeletedAt;

    /**
     * 商户接单记录：操作人和接单时间。
     */
    private Long acceptOperatorId;

    private LocalDateTime acceptedAt;

    /**
     * 关单原因：PAY_TIMEOUT（支付超时）、USER_CANCEL（用户取消）、MERCHANT_CANCEL（商户取消）等。
     */
    private String closeReason;

    /**
     * 关单时间。
     */
    private LocalDateTime closedAt;

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
                item.recalculateAmounts();
                BigDecimal unitPrice = defaultBigDecimal(item.getUnitPrice());
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
     * <p>使用 Canonical 状态 WAIT_PAY，避免重复语义。</p>
     */
    public void markPendingPayment() {
        this.status = OrderStatus.WAIT_PAY;
        this.payStatus = PayStatus.UNPAID;
    }

    /**
     * 标记为已支付。
     * <p>使用 Canonical 状态判断，自动兼容 CANCELLED 的重复语义。</p>
     */
    public void markPaid() {
        this.payStatus = PayStatus.PAID;
        // 使用 normalize 统一判断，自动兼容 CANCELLED 等重复语义状态
        OrderStatus canonical = this.status != null ? this.status.normalize() : null;
        if (canonical != OrderStatus.CANCELED 
                && canonical != OrderStatus.COMPLETED 
                && canonical != OrderStatus.REFUNDED) {
            this.status = OrderStatus.WAIT_ACCEPT;
        }
    }

    /**
     * 由支付链路调用，记录支付单信息并流转状态。
     * <p>状态机约束：只允许从 WAIT_PAY 状态流转到 PAID 状态。</p>
     * <p>幂等性：如果订单已经是 PAID 状态，则直接返回，不抛异常（允许重复回调）。</p>
     * 
     * @param payOrderId 支付单ID
     * @param payChannel 支付渠道（如：WECHAT_JSAPI、ALIPAY_WAP）
     * @param payNo 渠道支付单号（如：微信transaction_id）
     * @param paidAt 支付完成时间
     * @throws BizException 如果订单状态不允许支付（非 WAIT_PAY 且非 PAID）
     */
    public void markPaid(Long payOrderId, String payChannel, String payNo, LocalDateTime paidAt) {
        // 幂等性：如果订单已经是 PAID 状态，直接返回（允许重复回调）
        if (OrderStatus.PAID.equals(this.status)) {
            log.debug("订单已支付，幂等返回：orderId={}, status={}", this.id, this.status);
            return;
        }
        
        // 状态机约束：只允许从 WAIT_PAY 状态流转到 PAID 状态
        if (!OrderStatus.WAIT_PAY.equals(this.status)) {
            String msg = String.format("订单状态不允许支付：当前状态=%s，只允许 WAIT_PAY 状态支付", 
                    this.status != null ? this.status.getCode() : "NULL");
            log.warn("订单状态不允许支付：orderId={}, currentStatus={}", this.id, this.status);
            throw new BizException(CommonErrorCode.BAD_REQUEST, msg);
        }
        
        // 流转状态：WAIT_PAY -> PAID
        this.status = OrderStatus.PAID;
        this.payStatus = PayStatus.PAID;
        
        // 记录支付信息到扩展字段
        updateExt("payOrderId", payOrderId);
        updateExt("payChannel", payChannel);
        updateExt("payNo", payNo);
        updateExt("paidAt", paidAt != null ? paidAt.toString() : null);
        
        this.updatedAt = LocalDateTime.now();
        
        log.info("订单支付成功：orderId={}, payOrderId={}, payChannel={}, payNo={}", 
                this.id, payOrderId, payChannel, payNo);
    }
    
    /**
     * 由支付链路调用，记录支付单信息并流转状态（兼容旧版本）。
     */
    public void markPaid(Long payOrderId, Long paidAmountCents) {
        markPaid();
        updateExt("payOrderId", payOrderId);
        updateExt("paidAmountCents", paidAmountCents);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 商户接单：只允许从 WAIT_ACCEPT 状态进入 ACCEPTED，重复接单直接返回。
     */
    public void accept(Long operatorId) {
        if (OrderStatus.ACCEPTED.equals(this.status)) {
            return;
        }
        if (this.status == null || !this.status.canAccept()) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "ORDER_STATUS_NOT_ALLOW_ACCEPT");
        }
        this.status = OrderStatus.ACCEPTED;
        this.acceptOperatorId = operatorId;
        LocalDateTime now = LocalDateTime.now();
        this.acceptedAt = now;
        this.updatedAt = now;
    }

    /**
     * 标记为已取消。
     * <p>使用 Canonical 状态 CANCELED，避免重复语义。</p>
     */
    public void markCancelled() {
        this.status = OrderStatus.CANCELED;
        if (this.payStatus == null) {
            this.payStatus = PayStatus.UNPAID;
        }
    }
    
    /**
     * 标记为已取消（带关单原因）。
     * <p>用于超时关单、用户取消、商户取消等场景。</p>
     * <p>幂等性：如果订单已经是 CANCELED 状态，则直接返回。</p>
     * <p>使用 Canonical 状态和 normalize() 统一判断，自动兼容 CANCELLED 等重复语义。</p>
     * 
     * @param closeReason 关单原因（PAY_TIMEOUT、USER_CANCEL、MERCHANT_CANCEL等）
     * @throws BizException 如果订单状态不允许取消（已支付、已完成等）
     */
    public void markCancelledWithReason(String closeReason) {
        // 使用 normalize 统一判断，自动兼容 CANCELLED 等重复语义
        OrderStatus canonical = this.status != null ? this.status.normalize() : null;
        
        // 幂等性：如果订单已经是 CANCELED 状态，直接返回
        if (OrderStatus.CANCELED.equals(canonical)) {
            log.debug("订单已取消，幂等返回：orderId={}, closeReason={}", this.id, this.closeReason);
            return;
        }
        
        // 状态约束：只允许可取消状态进行取消操作
        if (this.status != null && !this.status.canCancel()) {
            String msg = String.format("订单状态不允许取消：当前状态=%s", 
                    this.status.getCode());
            log.warn("订单状态不允许取消：orderId={}, currentStatus={}", this.id, this.status);
            throw new BizException(CommonErrorCode.BAD_REQUEST, msg);
        }
        
        // 流转状态（使用 Canonical 状态）
        this.status = OrderStatus.CANCELED;
        if (this.payStatus == null) {
            this.payStatus = PayStatus.UNPAID;
        }
        
        // 记录关单原因和时间
        this.closeReason = closeReason;
        this.closedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        log.info("订单已取消：orderId={}, closeReason={}, closedAt={}", 
                this.id, closeReason, this.closedAt);
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

    /**
     * 用户侧是否可取消。
     * <p>使用 canCancel() 统一判断，自动兼容重复语义状态。</p>
     * <p>草稿态（DRAFT/LOCKED_FOR_CHECKOUT/PENDING_CONFIRM）在 normalize 后会映射为 WAIT_PAY，允许取消。</p>
     */
    public boolean canCancelByUser() {
        if (status == null) {
            return false;
        }
        // 使用 canCancel 统一判断，自动兼容 PENDING_PAYMENT/PENDING_ACCEPT/DRAFT 等
        return status.canCancel();
    }

    /**
     * 用户侧取消订单。
     * <p>使用 Canonical 状态 CANCELED，避免重复语义。</p>
     */
    public void cancelByUser() {
        if (!canCancelByUser()) {
            throw new IllegalStateException("当前状态不允许用户取消订单");
        }
        this.status = OrderStatus.CANCELED;
        if (this.payStatus == null) {
            this.payStatus = PayStatus.UNPAID;
        }
    }

    public boolean isUserDeleted() {
        return Boolean.TRUE.equals(this.userDeleted);
    }

    public void markUserDeleted() {
        this.userDeleted = true;
        this.userDeletedAt = LocalDateTime.now();
    }

    /**
     * 当前订单是否处于草稿态。
     */
    public boolean isDraft() {
        return OrderStatus.DRAFT.equals(this.status);
    }

    /**
     * 确保当前订单可编辑（必须是草稿态）。
     *
     * @throws BizException 非草稿态时抛出
     */
    public void assertEditable() {
        if (!isDraft()) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单当前状态不可编辑购物车");
        }
    }

    /**
     * 新增或合并一个购物车行。
     */
    public void addOrMergeItem(OrderItem newItem) {
        assertEditable();
        if (newItem == null) {
            return;
        }
        List<OrderItem> working = ensureMutableItems();
        OrderItem existing = findSameLineItem(working, newItem);
        if (existing != null) {
            int currentQty = existing.getQuantity() == null ? 0 : existing.getQuantity();
            int delta = newItem.getQuantity() == null ? 0 : newItem.getQuantity();
            existing.setQuantity(currentQty + delta);
            existing.recalculateAmounts();
        } else {
            newItem.recalculateAmounts();
            working.add(newItem);
        }
        this.items = working;
        recalculateAmounts();
    }

    /**
     * 修改某个 SKU 行的数量。
     */
    public void changeItemQuantity(Long skuId, Map<String, Object> attrs, int newQuantity) {
        assertEditable();
        if (newQuantity <= 0) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "明细数量必须大于0");
        }
        List<OrderItem> working = ensureMutableItems();
        OrderItem target = findLineItem(working, skuId, attrs);
        if (target == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "待修改的购物车明细不存在");
        }
        target.setQuantity(newQuantity);
        target.recalculateAmounts();
        this.items = working;
        recalculateAmounts();
    }

    /**
     * 删除指定的购物车行。
     */
    public void removeItem(Long skuId, Map<String, Object> attrs) {
        assertEditable();
        List<OrderItem> working = ensureMutableItems();
        OrderItem target = findLineItem(working, skuId, attrs);
        if (target == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "待删除的购物车明细不存在");
        }
        working.remove(target);
        this.items = working;
        recalculateAmounts();
    }

    /**
     * 清空购物车明细。
     */
    public void clearItems() {
        assertEditable();
        this.items = new ArrayList<>();
        recalculateAmounts();
    }

    /**
     * 将草稿订单确认为正式订单。
     * <p>使用 Canonical 状态 WAIT_PAY，避免重复语义。</p>
     */
    public void confirmFromDraft(SubmitOrderFromDraftCommand command) {
        if (command == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "确认订单命令不能为空");
        }
        if (!isDraft() && !OrderStatus.LOCKED_FOR_CHECKOUT.equals(this.status)) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单当前状态不可提交");
        }
        if (items == null || items.isEmpty()) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单无商品，无法提交");
        }
        recalculateAmounts();
        BigDecimal clientAmount = Objects.requireNonNull(command.getClientPayableAmount(), "clientPayableAmount");
        BigDecimal diff = payableAmount.subtract(clientAmount).abs();
        if (diff.compareTo(CONFIRM_TOLERANCE) > 0) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单金额有变化，请刷新后重试");
        }
        this.payStatus = PayStatus.UNPAID;
        // 使用 Canonical 状态 WAIT_PAY 替代 PENDING_PAYMENT
        this.status = OrderStatus.WAIT_PAY;
        this.updatedAt = LocalDateTime.now();
        if (command.getUserRemark() != null && !command.getUserRemark().isBlank()) {
            this.remark = command.getUserRemark();
        }
        updateExt("orderToken", command.getOrderToken());
        updateExt("contactName", command.getContactName());
        updateExt("contactPhone", command.getContactPhone());
        updateExt("addressJson", command.getAddressJson());
    }

    private void updateExt(String key, Object value) {
        if (value == null) {
            return;
        }
        Map<String, Object> copy = new HashMap<>(this.ext == null ? Collections.emptyMap() : this.ext);
        copy.put(key, value);
        this.ext = copy;
    }

    private OrderItem findSameLineItem(List<OrderItem> candidates, OrderItem item) {
        if (candidates == null || item == null) {
            return null;
        }
        for (OrderItem candidate : candidates) {
            if (candidate != null && candidate.sameCartLine(item.getSkuId(), item.getAttrs())) {
                return candidate;
            }
        }
        return null;
    }

    private OrderItem findLineItem(List<OrderItem> candidates, Long skuId, Map<String, Object> attrs) {
        if (candidates == null) {
            return null;
        }
        for (OrderItem candidate : candidates) {
            if (candidate != null && candidate.sameCartLine(skuId, attrs)) {
                return candidate;
            }
        }
        return null;
    }

    private List<OrderItem> ensureMutableItems() {
        if (this.items == null) {
            this.items = new ArrayList<>();
            return this.items;
        }
        if (this.items instanceof ArrayList) {
            return this.items;
        }
        List<OrderItem> copy = new ArrayList<>(this.items);
        this.items = copy;
        return copy;
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 根据确认订单命令创建聚合根，并完成金额初始值的计算。
     */
    public static Order createFromConfirmCommand(ConfirmOrderCommand command) {
        if (command == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "确认订单命令不能为空");
        }
        if (command.getTenantId() == null || command.getStoreId() == null || command.getUserId() == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "租户/门店/用户信息不能为空");
        }
        if (command.getItems() == null || command.getItems().isEmpty()) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单明细不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        List<OrderItem> items = command.getItems().stream()
                .filter(Objects::nonNull)
                .map(dto -> OrderItem.fromConfirmItem(dto, command.getTenantId(), command.getStoreId(), now))
                .collect(Collectors.toList());
        Order order = Order.builder()
                .tenantId(command.getTenantId())
                .storeId(command.getStoreId())
                .userId(command.getUserId())
                .clientOrderNo(command.getClientOrderNo())
                .channel(command.getChannel())
                .bizType(BizType.fromCode(command.getBizType()))
                .orderSource(OrderSource.fromCode(command.getOrderSource()))
                .currency(StringUtils.hasText(command.getCurrency()) ? command.getCurrency() : "CNY")
                .remark(command.getRemark())
                .status(OrderStatus.INIT)
                .payStatus(PayStatus.UNPAID)
                .items(items)
                .version(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        order.setDiscountAmount(defaultNumber(command.getClientDiscountAmount()));
        order.recalculateAmounts();
        return order;
    }

    private static BigDecimal defaultNumber(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 将订单标记为已创建，流转到 WAIT_PAY 状态。
     */
    public void markCreated() {
        this.status = OrderStatus.WAIT_PAY;
        this.payStatus = PayStatus.UNPAID;
        this.updatedAt = LocalDateTime.now();
    }
}
