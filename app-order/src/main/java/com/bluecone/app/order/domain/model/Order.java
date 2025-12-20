package com.bluecone.app.order.domain.model;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
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

/**
 * 订单聚合根
 * 
 * <p>订单是系统的核心业务实体，代表用户的一次购买行为。
 * 作为DDD聚合根，Order负责维护订单的完整性和业务规则。
 * 
 * <h3>聚合边界</h3>
 * <p>Order聚合包含以下实体和值对象：</p>
 * <ul>
 *   <li>{@link OrderItem} - 订单明细项（聚合内实体）</li>
 *   <li>{@link OrderStatus} - 订单状态（值对象）</li>
 *   <li>{@link PayStatus} - 支付状态（值对象）</li>
 *   <li>{@link BizType} - 业务类型（值对象）</li>
 *   <li>{@link OrderSource} - 订单来源（值对象）</li>
 * </ul>
 * 
 * <h3>核心业务能力</h3>
 * <ul>
 *   <li><b>订单创建</b>：从草稿转正式订单、从购物车创建订单</li>
 *   <li><b>金额计算</b>：根据明细项自动计算总额、优惠额、应付额</li>
 *   <li><b>状态流转</b>：支付、接单、制作、出餐、完成、取消、退款等</li>
 *   <li><b>业务规则校验</b>：状态前置条件、金额校验、并发控制</li>
 *   <li><b>会话管理</b>：支持购物车会话、防止重复提交</li>
 * </ul>
 * 
 * <h3>状态机</h3>
 * <p>订单的生命周期遵循以下状态流转：</p>
 * <pre>
 * WAIT_PAY（待支付）
 *   ↓ 支付成功
 * WAIT_ACCEPT（待接单）
 *   ↓ 商户接单
 * ACCEPTED（已接单）
 *   ↓ 开始制作
 * IN_PROGRESS（制作中）
 *   ↓ 制作完成
 * READY（已出餐）
 *   ↓ 用户取货
 * COMPLETED（已完成）
 * 
 * 任意非终态 → CANCELED（已取消）
 * 已完成 → REFUNDED（已退款）
 * 超时/异常 → CLOSED（已关闭）
 * </pre>
 * 
 * <h3>多租户隔离</h3>
 * <p>所有订单操作都基于tenantId进行数据隔离，确保租户间数据安全。</p>
 * 
 * <h3>并发控制</h3>
 * <p>使用version字段实现乐观锁，防止并发更新导致的数据不一致。</p>
 * 
 * <h3>审计追踪</h3>
 * <p>记录订单的完整操作历史，包括：</p>
 * <ul>
 *   <li>创建人、创建时间</li>
 *   <li>更新人、更新时间</li>
 *   <li>接单人、接单时间</li>
 *   <li>拒单人、拒单原因、拒单时间</li>
 *   <li>取消人、取消原因、取消时间</li>
 *   <li>各状态流转时间点</li>
 * </ul>
 * 
 * @author BlueCone
 * @since 1.0.0
 * @see OrderItem
 * @see OrderStatus
 * @see PayStatus
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;
    
    /**
     * 订单确认时的金额容差（0.01元）
     * 用于处理浮点数计算精度问题，当计算金额与预期金额差异在容差范围内时视为一致
     */
    private static final BigDecimal CONFIRM_TOLERANCE = new BigDecimal("0.01");

    // ========== 标识与多租户 ==========
    
    /**
     * 订单ID（主键）
     * 使用ULID生成，具有时间有序性和全局唯一性
     */
    private Long id;

    /**
     * 租户ID
     * 用于多租户数据隔离，所有订单操作都基于租户上下文
     */
    private Long tenantId;

    /**
     * 门店ID
     * 订单归属的门店，用于门店维度的订单管理和统计
     */
    private Long storeId;

    // ========== 用户 & 会话信息 ==========
    
    /**
     * 用户ID
     * 下单用户的唯一标识
     */
    private Long userId;

    /**
     * 会话ID
     * 用于关联购物车会话，防止重复提交和会话冲突
     */
    private String sessionId;

    /**
     * 会话版本号
     * 用于购物车会话的乐观锁控制，防止并发修改
     */
    @Builder.Default
    private Integer sessionVersion = 0;

    /**
     * 订单编号
     * 系统生成的唯一订单号，用于对外展示和查询
     * 格式：yyyyMMddHHmmss + 随机数
     */
    private String orderNo;

    /**
     * 客户端订单号
     * 客户端生成的幂等键，用于防止重复下单
     */
    private String clientOrderNo;

    // ========== 业务维度 ==========
    
    /**
     * 业务类型
     * 区分不同的业务场景，如堂食、外卖、自提等
     * @see BizType
     */
    private BizType bizType;

    /**
     * 订单来源
     * 标识订单的来源渠道，如小程序、H5、APP等
     * @see OrderSource
     */
    private OrderSource orderSource;

    /**
     * 渠道标识
     * 更细粒度的渠道标识，如具体的小程序AppId
     */
    private String channel;

    // ========== 金额 ==========
    
    /**
     * 订单总金额
     * 所有订单明细的原价总和（未扣除优惠）
     * 计算公式：∑(item.unitPrice * item.quantity)
     */
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * 优惠金额
     * 所有优惠的总和，包括优惠券、活动折扣、会员折扣等
     * 计算公式：totalAmount - payableAmount
     */
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * 应付金额
     * 用户实际需要支付的金额
     * 计算公式：totalAmount - discountAmount
     */
    @Builder.Default
    private BigDecimal payableAmount = BigDecimal.ZERO;

    /**
     * 货币类型
     * 默认为人民币（CNY），支持多币种扩展
     */
    @Builder.Default
    private String currency = "CNY";

    // ========== 优惠券 ==========
    
    /**
     * 优惠券ID
     * 用户使用的优惠券ID，用于关联优惠券使用记录
     */
    private Long couponId;

    // ========== 状态 ==========
    
    /**
     * 订单主状态
     * 订单的核心状态，决定订单的生命周期流转
     * @see OrderStatus
     */
    private OrderStatus status;

    /**
     * 支付状态
     * 独立的支付状态，与订单主状态解耦
     * @see PayStatus
     */
    private PayStatus payStatus;

    // ========== 备注 & 扩展 ==========
    
    /**
     * 订单备注
     * 用户填写的订单备注信息，如口味要求、配送要求等
     */
    private String remark;

    /**
     * 扩展字段
     * 用于存储业务特定的扩展信息，以JSON格式存储
     * 避免频繁修改表结构，提高系统扩展性
     */
    @Builder.Default
    private Map<String, Object> ext = Collections.emptyMap();

    // ========== 聚合内明细 ==========
    
    /**
     * 订单明细列表
     * 订单包含的商品明细项，作为聚合内实体
     * @see OrderItem
     */
    @Builder.Default
    private List<OrderItem> items = Collections.emptyList();

    // ========== 版本和审计字段 ==========
    
    /**
     * 版本号
     * 用于乐观锁并发控制，每次更新递增
     */
    @Builder.Default
    private Integer version = 0;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 更新人ID
     */
    private Long updatedBy;

    /**
     * 用户删除标记
     * 用户端软删除标记，用户删除后订单对用户不可见，但系统仍保留
     */
    private Boolean userDeleted;

    /**
     * 用户删除时间
     */
    private LocalDateTime userDeletedAt;

    // ========== 商户接单记录 ==========
    
    /**
     * 接单操作人ID
     * 商户端接单操作的员工ID，用于追踪接单责任人
     */
    private Long acceptOperatorId;

    /**
     * 接单时间
     * 商户确认接单的时间点，用于计算接单时效
     */
    private LocalDateTime acceptedAt;

    // ========== 商户拒单记录 ==========
    
    /**
     * 拒单原因码
     * 标准化的拒单原因代码，如：
     * <ul>
     *   <li>OUT_OF_STOCK - 商品缺货</li>
     *   <li>STORE_BUSY - 门店繁忙</li>
     *   <li>INVALID_ADDRESS - 配送地址无效</li>
     *   <li>OTHER - 其他原因</li>
     * </ul>
     */
    private String rejectReasonCode;

    /**
     * 拒单原因描述
     * 商户填写的具体拒单原因，用于向用户解释
     */
    private String rejectReasonDesc;

    /**
     * 拒单时间
     * 商户拒绝接单的时间点
     */
    private LocalDateTime rejectedAt;

    /**
     * 拒单操作人ID
     * 执行拒单操作的员工ID
     */
    private Long rejectedBy;

    // ========== 关单记录 ==========
    
    /**
     * 关单原因
     * 订单关闭的原因代码，如：
     * <ul>
     *   <li>PAY_TIMEOUT - 支付超时自动关闭</li>
     *   <li>USER_CANCEL - 用户主动取消</li>
     *   <li>MERCHANT_CANCEL - 商户取消</li>
     *   <li>SYSTEM_CANCEL - 系统异常关闭</li>
     * </ul>
     */
    private String closeReason;

    /**
     * 关单时间
     * 订单关闭的时间点
     */
    private LocalDateTime closedAt;

    // ========== 订单履约时间点 ==========
    
    /**
     * 开始制作时间
     * 状态从ACCEPTED流转到IN_PROGRESS的时间点
     * 用于计算制作时效和预计完成时间
     */
    private LocalDateTime startedAt;

    /**
     * 出餐时间
     * 状态从IN_PROGRESS流转到READY的时间点
     * 表示商品已制作完成，可以取货或配送
     */
    private LocalDateTime readyAt;

    /**
     * 完成时间
     * 状态流转到COMPLETED的时间点
     * 表示订单履约完成，用户已取货或已送达
     */
    private LocalDateTime completedAt;

    /**
     * 最近一次状态变化时间
     * 记录订单状态最后一次变更的时间
     * 用途：
     * <ul>
     *   <li>SLA统计：计算各环节耗时</li>
     *   <li>超时判断：判断订单是否超时</li>
     *   <li>异常监控：识别长时间未流转的订单</li>
     * </ul>
     */
    private LocalDateTime lastStateChangedAt;

    /**
     * 最近操作人ID
     * 最后一次操作订单的人员ID
     * 包括接单、开始制作、出餐、完成等操作
     */
    private Long operatorId;

    // ========== 取消记录 ==========
    
    /**
     * 取消时间
     * 订单被取消的时间点
     * 适用于用户取消、商户拒单等场景
     */
    private LocalDateTime canceledAt;

    /**
     * 取消原因码
     * 标准化的取消原因代码，如：
     * <ul>
     *   <li>USER_CANCEL - 用户主动取消</li>
     *   <li>MERCHANT_REJECT - 商户拒单</li>
     *   <li>PAY_TIMEOUT - 支付超时</li>
     *   <li>DUPLICATE_ORDER - 重复下单</li>
     *   <li>OTHER - 其他原因</li>
     * </ul>
     */
    private String cancelReasonCode;

    /**
     * 取消原因描述
     * 用户或商户填写的具体取消原因
     * 用于客服分析和用户反馈
     */
    private String cancelReasonDesc;

    // ========== 退款记录 ==========
    
    /**
     * 退款时间
     * 退款成功的时间点
     * 仅在订单发生退款时填充
     */
    private LocalDateTime refundedAt;

    /**
     * 退款单ID
     * 关联的退款单主键（bc_refund_order.id）
     * 用于追溯退款详情和退款流水
     */
    private Long refundOrderId;

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
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "订单金额校验失败，请刷新后重试");
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
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, msg);
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
     * 
     * <h4>为什么该状态允许接单：</h4>
     * <ul>
     *   <li>WAIT_ACCEPT（待接单）：订单已支付，等待商户确认接单，这是接单的唯一合法前置状态</li>
     *   <li>ACCEPTED（已接单）：幂等性保护，重复接单不报错，直接返回</li>
     * </ul>
     * 
     * <h4>并发冲突时为什么要失败而不是覆盖：</h4>
     * <ul>
     *   <li>防止状态回退：如果订单已被其他操作员接单或拒单，当前操作应该失败，避免覆盖已有的接单/拒单记录</li>
     *   <li>业务正确性：商户接单是关键业务节点，需要精确控制状态流转，避免多人同时操作导致状态混乱</li>
     *   <li>审计追溯：乐观锁失败时抛出异常，前端可提示用户刷新，确保用户看到的是最新状态</li>
     * </ul>
     * 
     * @param operatorId 操作人ID
     * @throws BizException 如果订单状态不允许接单（非 WAIT_ACCEPT 且非 ACCEPTED）
     */
    public void accept(Long operatorId) {
        // 幂等性：如果订单已接单，直接返回
        if (OrderStatus.ACCEPTED.equals(this.status)) {
            log.debug("订单已接单，幂等返回：orderId={}, acceptedAt={}", this.id, this.acceptedAt);
            return;
        }
        
        // 状态约束：只允许 WAIT_ACCEPT 状态的订单接单
        if (this.status == null || !this.status.canAccept()) {
            String currentStatus = this.status != null ? this.status.getCode() : "NULL";
            String msg = String.format("订单状态不允许接单：当前状态=%s，只允许 WAIT_ACCEPT 状态接单", currentStatus);
            log.warn("订单状态不允许接单：orderId={}, currentStatus={}", this.id, currentStatus);
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, msg);
        }
        
        // 流转状态：WAIT_ACCEPT -> ACCEPTED
        this.status = OrderStatus.ACCEPTED;
        this.acceptOperatorId = operatorId;
        LocalDateTime now = LocalDateTime.now();
        this.acceptedAt = now;
        this.updatedAt = now;
        
        log.info("订单接单成功：orderId={}, operatorId={}, acceptedAt={}", this.id, operatorId, now);
    }

    /**
     * 商户拒单：只允许从 WAIT_ACCEPT 状态进入 CANCELED，重复拒单直接返回。
     * 
     * <h4>为什么该状态允许拒单：</h4>
     * <ul>
     *   <li>WAIT_ACCEPT（待接单）：订单已支付但商户尚未接单，商户有权拒绝接单（如库存不足、太忙等）</li>
     *   <li>CANCELED（已取消）：幂等性保护，如果订单已拒单（状态为 CANCELED 且有拒单原因），直接返回</li>
     * </ul>
     * 
     * <h4>为什么选择 CANCELED 而不是 CLOSED：</h4>
     * <ul>
     *   <li>语义明确：CANCELED 表示订单被取消（包括用户取消、商户拒单、超时取消），CLOSED 表示订单关闭（更通用）</li>
     *   <li>一致性：用户取消订单时也使用 CANCELED 状态，商户拒单本质上是另一种取消方式</li>
     *   <li>退款触发：CANCELED 状态会触发退款流程，确保用户已支付的款项能够退回</li>
     * </ul>
     * 
     * <h4>并发冲突时为什么要失败而不是覆盖：</h4>
     * <ul>
     *   <li>防止状态回退：如果订单已被接单（ACCEPTED）或已拒单，当前拒单操作应该失败，避免覆盖</li>
     *   <li>业务正确性：不能在订单已接单后再拒单，这会导致业务流程混乱</li>
     *   <li>审计追溯：保留第一次拒单的原因和操作人，避免被后续操作覆盖</li>
     * </ul>
     * 
     * @param operatorId 操作人ID
     * @param reasonCode 拒单原因码（如：OUT_OF_STOCK、BUSY、OTHER）
     * @param reasonDesc 拒单原因说明（商户填写的具体原因）
     * @throws BizException 如果订单状态不允许拒单（非 WAIT_ACCEPT 且非已拒单状态）
     */
    public void reject(Long operatorId, String reasonCode, String reasonDesc) {
        // 使用 normalize 统一判断，自动兼容 CANCELLED 等重复语义
        OrderStatus canonical = this.status != null ? this.status.normalize() : null;
        
        // 幂等性：如果订单已拒单（状态为 CANCELED 且有拒单原因码），直接返回
        if (OrderStatus.CANCELED.equals(canonical) && this.rejectReasonCode != null) {
            log.debug("订单已拒单，幂等返回：orderId={}, rejectReasonCode={}, rejectedAt={}", 
                    this.id, this.rejectReasonCode, this.rejectedAt);
            return;
        }
        
        // 状态约束：只允许 WAIT_ACCEPT 状态的订单拒单
        if (this.status == null || !this.status.canAccept()) {
            String currentStatus = this.status != null ? this.status.getCode() : "NULL";
            String msg = String.format("订单状态不允许拒单：当前状态=%s，只允许 WAIT_ACCEPT 状态拒单", currentStatus);
            log.warn("订单状态不允许拒单：orderId={}, currentStatus={}", this.id, currentStatus);
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, msg);
        }
        
        // 流转状态：WAIT_ACCEPT -> CANCELED
        this.status = OrderStatus.CANCELED;
        this.rejectReasonCode = reasonCode;
        this.rejectReasonDesc = reasonDesc;
        this.rejectedBy = operatorId;
        LocalDateTime now = LocalDateTime.now();
        this.rejectedAt = now;
        this.updatedAt = now;
        
        // 同时设置关单原因为商户拒单
        this.closeReason = "MERCHANT_REJECT";
        this.closedAt = now;
        
        log.info("订单拒单成功：orderId={}, operatorId={}, reasonCode={}, reasonDesc={}, rejectedAt={}", 
                this.id, operatorId, reasonCode, reasonDesc, now);
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
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, msg);
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

    /**
     * 商户开始制作：只允许从 ACCEPTED 状态进入 IN_PROGRESS。
     * 
     * <h4>为什么该状态允许开始制作：</h4>
     * <ul>
     *   <li>ACCEPTED（已接单）：商户已接单，可以开始制作，这是开始制作的唯一合法前置状态</li>
     *   <li>IN_PROGRESS（制作中）：幂等性保护，重复开始不报错，直接返回</li>
     * </ul>
     * 
     * <h4>为什么其他状态不允许开始制作：</h4>
     * <ul>
     *   <li>WAIT_ACCEPT（待接单）：商户尚未接单，不能开始制作</li>
     *   <li>READY（已出餐）：订单已出餐，不能回退到制作中</li>
     *   <li>COMPLETED（已完成）：订单已完成，不能回退到制作中</li>
     *   <li>CANCELED（已取消）：订单已取消，不能再制作</li>
     * </ul>
     * 
     * @param operatorId 操作人ID
     * @param now 当前时间
     * @throws BizException 如果订单状态不允许开始制作
     */
    public void start(Long operatorId, LocalDateTime now) {
        // 使用 normalize 统一判断，自动兼容重复语义状态
        OrderStatus canonical = this.status != null ? this.status.normalize() : null;
        
        // 幂等性：如果订单已在制作中，直接返回
        if (OrderStatus.IN_PROGRESS.equals(canonical)) {
            log.debug("订单已在制作中，幂等返回：orderId={}, startedAt={}", this.id, this.startedAt);
            return;
        }
        
        // 状态约束：只允许 ACCEPTED 状态的订单开始制作
        if (!OrderStatus.ACCEPTED.equals(canonical)) {
            String currentStatus = this.status != null ? this.status.getCode() : "NULL";
            String msg = String.format("订单状态不允许开始制作：当前状态=%s，只允许 ACCEPTED 状态开始制作", currentStatus);
            log.warn("订单状态不允许开始制作：orderId={}, currentStatus={}", this.id, currentStatus);
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, msg);
        }
        
        // 流转状态：ACCEPTED -> IN_PROGRESS
        this.status = OrderStatus.IN_PROGRESS;
        this.startedAt = now;
        this.lastStateChangedAt = now;
        this.operatorId = operatorId;
        this.updatedAt = now;
        
        log.info("订单开始制作：orderId={}, operatorId={}, startedAt={}", this.id, operatorId, now);
    }

    /**
     * 商户出餐完成：只允许从 IN_PROGRESS 状态进入 READY。
     * 
     * <h4>为什么该状态允许出餐完成：</h4>
     * <ul>
     *   <li>IN_PROGRESS（制作中）：订单正在制作，可以标记出餐完成，这是出餐的唯一合法前置状态</li>
     *   <li>READY（已出餐）：幂等性保护，重复出餐不报错，直接返回</li>
     * </ul>
     * 
     * <h4>为什么其他状态不允许出餐完成：</h4>
     * <ul>
     *   <li>ACCEPTED（已接单）：订单尚未开始制作，不能直接出餐</li>
     *   <li>COMPLETED（已完成）：订单已完成，不能回退到出餐</li>
     *   <li>CANCELED（已取消）：订单已取消，不能再出餐</li>
     * </ul>
     * 
     * @param operatorId 操作人ID
     * @param now 当前时间
     * @throws BizException 如果订单状态不允许出餐完成
     */
    public void markReady(Long operatorId, LocalDateTime now) {
        // 使用 normalize 统一判断，自动兼容重复语义状态
        OrderStatus canonical = this.status != null ? this.status.normalize() : null;
        
        // 幂等性：如果订单已出餐，直接返回
        if (OrderStatus.READY.equals(canonical)) {
            log.debug("订单已出餐，幂等返回：orderId={}, readyAt={}", this.id, this.readyAt);
            return;
        }
        
        // 状态约束：只允许 IN_PROGRESS 状态的订单出餐完成
        if (!OrderStatus.IN_PROGRESS.equals(canonical)) {
            String currentStatus = this.status != null ? this.status.getCode() : "NULL";
            String msg = String.format("订单状态不允许出餐完成：当前状态=%s，只允许 IN_PROGRESS 状态出餐完成", currentStatus);
            log.warn("订单状态不允许出餐完成：orderId={}, currentStatus={}", this.id, currentStatus);
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, msg);
        }
        
        // 流转状态：IN_PROGRESS -> READY
        this.status = OrderStatus.READY;
        this.readyAt = now;
        this.lastStateChangedAt = now;
        this.operatorId = operatorId;
        this.updatedAt = now;
        
        log.info("订单出餐完成：orderId={}, operatorId={}, readyAt={}", this.id, operatorId, now);
    }

    /**
     * 订单完成：只允许从 READY 状态进入 COMPLETED。
     * 
     * <h4>为什么该状态允许完成：</h4>
     * <ul>
     *   <li>READY（已出餐）：订单已出餐，用户已取餐/配送完成，可以标记为完成，这是完成的唯一合法前置状态</li>
     *   <li>COMPLETED（已完成）：幂等性保护，重复完成不报错，直接返回</li>
     * </ul>
     * 
     * <h4>为什么其他状态不允许完成：</h4>
     * <ul>
     *   <li>ACCEPTED（已接单）：订单尚未制作，不能直接完成</li>
     *   <li>IN_PROGRESS（制作中）：订单尚未出餐，不能直接完成</li>
     *   <li>CANCELED（已取消）：订单已取消，不能再完成</li>
     * </ul>
     * 
     * @param operatorId 操作人ID
     * @param now 当前时间
     * @throws BizException 如果订单状态不允许完成
     */
    public void complete(Long operatorId, LocalDateTime now) {
        // 使用 normalize 统一判断，自动兼容重复语义状态
        OrderStatus canonical = this.status != null ? this.status.normalize() : null;
        
        // 幂等性：如果订单已完成，直接返回
        if (OrderStatus.COMPLETED.equals(canonical)) {
            log.debug("订单已完成，幂等返回：orderId={}, completedAt={}", this.id, this.completedAt);
            return;
        }
        
        // 状态约束：只允许 READY 状态的订单完成
        if (!OrderStatus.READY.equals(canonical)) {
            String currentStatus = this.status != null ? this.status.getCode() : "NULL";
            String msg = String.format("订单状态不允许完成：当前状态=%s，只允许 READY 状态完成", currentStatus);
            log.warn("订单状态不允许完成：orderId={}, currentStatus={}", this.id, currentStatus);
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, msg);
        }
        
        // 流转状态：READY -> COMPLETED
        this.status = OrderStatus.COMPLETED;
        this.completedAt = now;
        this.lastStateChangedAt = now;
        this.operatorId = operatorId;
        this.updatedAt = now;
        
        log.info("订单完成：orderId={}, operatorId={}, completedAt={}", this.id, operatorId, now);
    }

    /**
     * 用户/系统取消订单：只允许从 WAIT_PAY、WAIT_ACCEPT、ACCEPTED 状态进入 CANCELED。
     * 
     * <h4>为什么这些状态允许取消：</h4>
     * <ul>
     *   <li>WAIT_PAY（待支付）：用户未支付，可随时取消，不涉及退款</li>
     *   <li>WAIT_ACCEPT（待接单）：用户已支付但商户未接单，可取消并退款</li>
     *   <li>ACCEPTED（已接单）：商户已接单但未开始制作，可协商取消并退款</li>
     *   <li>CANCELED（已取消）：幂等性保护，重复取消不报错，直接返回</li>
     * </ul>
     * 
     * <h4>为什么其他状态不允许取消：</h4>
     * <ul>
     *   <li>IN_PROGRESS（制作中）：商户已投入成本，默认不允许取消（M4 先不支持）</li>
     *   <li>READY（已出餐）：商品已制作完成，不允许取消</li>
     *   <li>COMPLETED（已完成）：订单已完成，不允许取消，只能发起退款</li>
     *   <li>REFUNDED（已退款）：已退款，不可再取消</li>
     *   <li>CLOSED（已关闭）：已关闭，不可再取消</li>
     * </ul>
     * 
     * <h4>并发控制：</h4>
     * <ul>
     *   <li>通过乐观锁（expectedVersion）保证并发安全</li>
     *   <li>通过幂等键（idemKey）保证同一请求只取消一次</li>
     * </ul>
     * 
     * @param reasonCode 取消原因码（USER_CANCEL、MERCHANT_REJECT、PAY_TIMEOUT等）
     * @param reasonDesc 取消原因描述（用户或商户填写的具体原因）
     * @param now 当前时间
     * @throws BizException 如果订单状态不允许取消
     */
    public void cancel(String reasonCode, String reasonDesc, LocalDateTime now) {
        // 使用 normalize 统一判断，自动兼容 CANCELLED 等重复语义
        OrderStatus canonical = this.status != null ? this.status.normalize() : null;
        
        // 幂等性：如果订单已取消，直接返回
        if (OrderStatus.CANCELED.equals(canonical)) {
            log.debug("订单已取消，幂等返回：orderId={}, cancelReasonCode={}, canceledAt={}", 
                    this.id, this.cancelReasonCode, this.canceledAt);
            return;
        }
        
        // 状态约束：只允许 WAIT_PAY、WAIT_ACCEPT、ACCEPTED 状态的订单取消
        if (this.status == null || !this.status.canCancel()) {
            String currentStatus = this.status != null ? this.status.getCode() : "NULL";
            String msg = String.format("订单状态不允许取消：当前状态=%s，只允许 WAIT_PAY/WAIT_ACCEPT/ACCEPTED 状态取消", currentStatus);
            log.warn("订单状态不允许取消：orderId={}, currentStatus={}", this.id, currentStatus);
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, msg);
        }
        
        // 流转状态：WAIT_PAY/WAIT_ACCEPT/ACCEPTED -> CANCELED
        this.status = OrderStatus.CANCELED;
        this.cancelReasonCode = reasonCode;
        this.cancelReasonDesc = reasonDesc;
        this.canceledAt = now;
        this.lastStateChangedAt = now;
        this.updatedAt = now;
        
        // 同时设置关单原因（兼容旧逻辑）
        this.closeReason = reasonCode;
        this.closedAt = now;
        
        log.info("订单已取消：orderId={}, reasonCode={}, reasonDesc={}, canceledAt={}", 
                this.id, reasonCode, reasonDesc, now);
    }

    /**
     * 标记为已退款：只允许"已支付且未退款"的订单进入 REFUNDED。
     * 
     * <h4>为什么需要严格的状态约束：</h4>
     * <ul>
     *   <li>防止重复退款：已退款的订单不能再次退款</li>
     *   <li>防止未支付订单退款：未支付的订单不能退款（直接取消即可）</li>
     *   <li>防止状态混乱：退款是终态操作，必须严格控制状态流转</li>
     * </ul>
     * 
     * <h4>允许流转到 REFUNDED 的前置状态：</h4>
     * <ul>
     *   <li>WAIT_ACCEPT：已支付但商户未接单，拒单后退款</li>
     *   <li>ACCEPTED：已接单但未开始制作，协商取消后退款</li>
     *   <li>CANCELED：已取消且已支付，需要退款</li>
     * </ul>
     * 
     * <h4>不允许流转到 REFUNDED 的状态：</h4>
     * <ul>
     *   <li>WAIT_PAY：未支付，不涉及退款（直接取消即可）</li>
     *   <li>IN_PROGRESS/READY：正在履约，不应直接退款（应先协商取消）</li>
     *   <li>COMPLETED：已完成，不应直接退款（应发起售后退款）</li>
     *   <li>REFUNDED：已退款，不可重复退款</li>
     *   <li>CLOSED：已关闭，不可再退款</li>
     * </ul>
     * 
     * <h4>并发控制：</h4>
     * <ul>
     *   <li>通过乐观锁（expectedVersion）保证并发安全</li>
     *   <li>通过幂等性保护（已退款时直接返回）避免重复退款</li>
     * </ul>
     * 
     * @param refundOrderId 退款单ID（关联bc_refund_order.id）
     * @param now 当前时间
     * @throws BizException 如果订单状态不允许退款
     */
    public void markRefunded(Long refundOrderId, LocalDateTime now) {
        // 使用 normalize 统一判断，自动兼容重复语义状态
        OrderStatus canonical = this.status != null ? this.status.normalize() : null;
        
        // 幂等性：如果订单已退款，直接返回
        if (OrderStatus.REFUNDED.equals(canonical)) {
            log.debug("订单已退款，幂等返回：orderId={}, refundOrderId={}, refundedAt={}", 
                    this.id, this.refundOrderId, this.refundedAt);
            return;
        }
        
        // 状态约束：只允许"已支付且未退款"的订单进入 REFUNDED
        // 允许的前置状态：WAIT_ACCEPT、ACCEPTED、CANCELED（已支付的情况下）
        if (canonical == null 
                || canonical == OrderStatus.WAIT_PAY  // 未支付，不涉及退款
                || canonical == OrderStatus.REFUNDED  // 已退款，不可重复
                || canonical == OrderStatus.CLOSED) {  // 已关闭，不可退款
            String currentStatus = this.status != null ? this.status.getCode() : "NULL";
            String msg = String.format("订单状态不允许退款：当前状态=%s，只允许已支付且未退款的订单退款", currentStatus);
            log.warn("订单状态不允许退款：orderId={}, currentStatus={}, payStatus={}", 
                    this.id, currentStatus, this.payStatus);
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, msg);
        }
        
        // 检查支付状态：必须已支付
        if (this.payStatus != PayStatus.PAID) {
            String msg = String.format("订单支付状态不允许退款：当前支付状态=%s，只允许已支付订单退款", 
                    this.payStatus != null ? this.payStatus.name() : "NULL");
            log.warn("订单支付状态不允许退款：orderId={}, payStatus={}", this.id, this.payStatus);
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, msg);
        }
        
        // 流转状态：WAIT_ACCEPT/ACCEPTED/CANCELED -> REFUNDED
        this.status = OrderStatus.REFUNDED;
        this.payStatus = PayStatus.REFUNDED;
        this.refundOrderId = refundOrderId;
        this.refundedAt = now;
        this.lastStateChangedAt = now;
        this.updatedAt = now;
        
        log.info("订单标记为已退款：orderId={}, refundOrderId={}, refundedAt={}", 
                this.id, refundOrderId, now);
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
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "订单当前状态不可编辑购物车");
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
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "明细数量必须大于0");
        }
        List<OrderItem> working = ensureMutableItems();
        OrderItem target = findLineItem(working, skuId, attrs);
        if (target == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "待修改的购物车明细不存在");
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
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "待删除的购物车明细不存在");
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
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "确认订单命令不能为空");
        }
        if (!isDraft() && !OrderStatus.LOCKED_FOR_CHECKOUT.equals(this.status)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "订单当前状态不可提交");
        }
        if (items == null || items.isEmpty()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "订单无商品，无法提交");
        }
        recalculateAmounts();
        BigDecimal clientAmount = Objects.requireNonNull(command.getClientPayableAmount(), "clientPayableAmount");
        BigDecimal diff = payableAmount.subtract(clientAmount).abs();
        if (diff.compareTo(CONFIRM_TOLERANCE) > 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "订单金额有变化，请刷新后重试");
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
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "确认订单命令不能为空");
        }
        if (command.getTenantId() == null || command.getStoreId() == null || command.getUserId() == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "租户/门店/用户信息不能为空");
        }
        if (command.getItems() == null || command.getItems().isEmpty()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "订单明细不能为空");
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
