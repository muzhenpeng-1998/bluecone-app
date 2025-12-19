package com.bluecone.app.order.domain.model;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.domain.enums.RefundChannel;
import com.bluecone.app.order.domain.enums.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 退款单聚合根。
 * 
 * <h3>领域职责：</h3>
 * <ul>
 *   <li>封装退款单的状态流转逻辑（INIT → PROCESSING → SUCCESS/FAILED）</li>
 *   <li>保证退款单的状态机约束（终态不可再流转）</li>
 *   <li>记录退款原因、退款金额、退款渠道等核心信息</li>
 *   <li>支持幂等性：通过 idemKey 保证同一请求只创建一个退款单</li>
 * </ul>
 * 
 * <h3>状态流转：</h3>
 * <ul>
 *   <li>INIT → PROCESSING：发起退款请求</li>
 *   <li>PROCESSING → SUCCESS：退款成功（收到回调）</li>
 *   <li>PROCESSING → FAILED：退款失败（收到回调或超时）</li>
 * </ul>
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundOrder implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // ========== 标识与多租户 ==========
    
    /**
     * 退款单ID（内部主键，ULID）。
     */
    private Long id;
    
    /**
     * 租户ID。
     */
    private Long tenantId;
    
    /**
     * 门店ID。
     */
    private Long storeId;
    
    /**
     * 订单ID（关联bc_order.id）。
     */
    private Long orderId;
    
    /**
     * 订单号（冗余，用于快速查询）。
     */
    private String publicOrderNo;
    
    // ========== 退款单编号 ==========
    
    /**
     * 退款单号（对外展示，PublicId格式：rfd_xxx）。
     */
    private String refundId;
    
    // ========== 退款渠道与金额 ==========
    
    /**
     * 退款渠道（WECHAT、ALIPAY、MOCK）。
     */
    private RefundChannel channel;
    
    /**
     * 退款金额（实际退款金额，单位：元）。
     */
    @Builder.Default
    private BigDecimal refundAmount = BigDecimal.ZERO;
    
    /**
     * 币种（默认：CNY）。
     */
    @Builder.Default
    private String currency = "CNY";
    
    // ========== 退款状态 ==========
    
    /**
     * 退款状态（INIT、PROCESSING、SUCCESS、FAILED）。
     */
    private RefundStatus status;
    
    /**
     * 第三方退款单号（如微信退款单号，退款成功后由回调填充）。
     */
    private String refundNo;
    
    // ========== 退款原因 ==========
    
    /**
     * 退款原因码（USER_CANCEL、MERCHANT_REJECT、OUT_OF_STOCK等）。
     */
    private String reasonCode;
    
    /**
     * 退款原因描述（用户或商户填写的具体原因）。
     */
    private String reasonDesc;
    
    // ========== 幂等键 ==========
    
    /**
     * 幂等键（格式：{tenantId}:{storeId}:{orderId}:refund:{requestId}）。
     * <p>用于保证同一请求只创建一个退款单。</p>
     */
    private String idemKey;
    
    // ========== 支付单信息 ==========
    
    /**
     * 支付单ID（关联bc_payment_order.id）。
     */
    private Long payOrderId;
    
    /**
     * 第三方支付单号（如微信transaction_id，用于退款时传给支付网关）。
     */
    private String payNo;
    
    // ========== 时间字段 ==========
    
    /**
     * 退款发起时间。
     */
    private LocalDateTime refundRequestedAt;
    
    /**
     * 退款完成时间（SUCCESS时填充）。
     */
    private LocalDateTime refundCompletedAt;
    
    // ========== 扩展字段 ==========
    
    /**
     * 扩展信息（存储第三方退款响应等）。
     */
    @Builder.Default
    private Map<String, Object> ext = Collections.emptyMap();
    
    // ========== 审计字段 ==========
    
    /**
     * 乐观锁版本号。
     */
    @Builder.Default
    private Integer version = 0;
    
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    
    /**
     * 创建人。
     */
    private Long createdBy;
    
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
    
    /**
     * 更新人。
     */
    private Long updatedBy;
    
    // ========== 领域行为 ==========
    
    /**
     * 标记为处理中：只允许从 INIT 状态流转到 PROCESSING。
     * 
     * <h4>为什么只允许 INIT → PROCESSING：</h4>
     * <ul>
     *   <li>INIT：退款单刚创建，可以发起退款请求</li>
     *   <li>PROCESSING：已在处理中，幂等返回</li>
     *   <li>SUCCESS/FAILED：终态，不可再流转</li>
     * </ul>
     * 
     * @param refundNo 第三方退款单号（如微信退款单号）
     * @param now 当前时间
     * @throws BizException 如果退款单状态不允许流转
     */
    public void markProcessing(String refundNo, LocalDateTime now) {
        // 幂等性：如果退款单已在处理中，直接返回
        if (RefundStatus.PROCESSING.equals(this.status)) {
            log.debug("退款单已在处理中，幂等返回：refundOrderId={}, refundNo={}", this.id, this.refundNo);
            return;
        }
        
        // 状态约束：只允许 INIT 状态的退款单流转到 PROCESSING
        if (!RefundStatus.INIT.equals(this.status)) {
            String currentStatus = this.status != null ? this.status.getCode() : "NULL";
            String msg = String.format("退款单状态不允许流转到PROCESSING：当前状态=%s，只允许 INIT 状态流转", currentStatus);
            log.warn("退款单状态不允许流转到PROCESSING：refundOrderId={}, currentStatus={}", this.id, currentStatus);
            throw new BizException(CommonErrorCode.BAD_REQUEST, msg);
        }
        
        // 流转状态：INIT -> PROCESSING
        this.status = RefundStatus.PROCESSING;
        this.refundNo = refundNo;
        this.updatedAt = now;
        
        log.info("退款单标记为处理中：refundOrderId={}, refundNo={}", this.id, refundNo);
    }
    
    /**
     * 标记为成功：只允许从 INIT 或 PROCESSING 状态流转到 SUCCESS。
     * 
     * <h4>为什么允许 INIT/PROCESSING → SUCCESS：</h4>
     * <ul>
     *   <li>INIT：退款单刚创建，直接收到成功回调（同步退款）</li>
     *   <li>PROCESSING：退款单已发起请求，收到成功回调（异步退款）</li>
     *   <li>SUCCESS：已成功，幂等返回</li>
     *   <li>FAILED：已失败，不允许覆盖（防止状态回退）</li>
     * </ul>
     * 
     * @param refundNo 第三方退款单号（如微信退款单号）
     * @param now 当前时间
     * @throws BizException 如果退款单状态不允许流转
     */
    public void markSuccess(String refundNo, LocalDateTime now) {
        // 幂等性：如果退款单已成功，直接返回
        if (RefundStatus.SUCCESS.equals(this.status)) {
            log.debug("退款单已成功，幂等返回：refundOrderId={}, refundNo={}, refundCompletedAt={}", 
                    this.id, this.refundNo, this.refundCompletedAt);
            return;
        }
        
        // 状态约束：只允许 INIT 或 PROCESSING 状态的退款单流转到 SUCCESS
        if (!RefundStatus.INIT.equals(this.status) && !RefundStatus.PROCESSING.equals(this.status)) {
            String currentStatus = this.status != null ? this.status.getCode() : "NULL";
            String msg = String.format("退款单状态不允许流转到SUCCESS：当前状态=%s，只允许 INIT/PROCESSING 状态流转", currentStatus);
            log.warn("退款单状态不允许流转到SUCCESS：refundOrderId={}, currentStatus={}", this.id, currentStatus);
            throw new BizException(CommonErrorCode.BAD_REQUEST, msg);
        }
        
        // 流转状态：INIT/PROCESSING -> SUCCESS
        this.status = RefundStatus.SUCCESS;
        this.refundNo = refundNo;
        this.refundCompletedAt = now;
        this.updatedAt = now;
        
        log.info("退款单标记为成功：refundOrderId={}, refundNo={}, refundCompletedAt={}", 
                this.id, refundNo, now);
    }
    
    /**
     * 标记为失败：只允许从 INIT 或 PROCESSING 状态流转到 FAILED。
     * 
     * <h4>为什么允许 INIT/PROCESSING → FAILED：</h4>
     * <ul>
     *   <li>INIT：退款单刚创建，直接收到失败回调或校验失败</li>
     *   <li>PROCESSING：退款单已发起请求，收到失败回调或超时</li>
     *   <li>FAILED：已失败，幂等返回</li>
     *   <li>SUCCESS：已成功，不允许覆盖（防止状态回退）</li>
     * </ul>
     * 
     * @param errorMsg 失败原因
     * @param now 当前时间
     * @throws BizException 如果退款单状态不允许流转
     */
    public void markFailed(String errorMsg, LocalDateTime now) {
        // 幂等性：如果退款单已失败，直接返回
        if (RefundStatus.FAILED.equals(this.status)) {
            log.debug("退款单已失败，幂等返回：refundOrderId={}, refundCompletedAt={}", 
                    this.id, this.refundCompletedAt);
            return;
        }
        
        // 状态约束：只允许 INIT 或 PROCESSING 状态的退款单流转到 FAILED
        if (!RefundStatus.INIT.equals(this.status) && !RefundStatus.PROCESSING.equals(this.status)) {
            String currentStatus = this.status != null ? this.status.getCode() : "NULL";
            String msg = String.format("退款单状态不允许流转到FAILED：当前状态=%s，只允许 INIT/PROCESSING 状态流转", currentStatus);
            log.warn("退款单状态不允许流转到FAILED：refundOrderId={}, currentStatus={}", this.id, currentStatus);
            throw new BizException(CommonErrorCode.BAD_REQUEST, msg);
        }
        
        // 流转状态：INIT/PROCESSING -> FAILED
        this.status = RefundStatus.FAILED;
        this.refundCompletedAt = now;
        this.updatedAt = now;
        
        // 记录失败原因到扩展字段
        updateExt("errorMsg", errorMsg);
        
        log.warn("退款单标记为失败：refundOrderId={}, errorMsg={}, refundCompletedAt={}", 
                this.id, errorMsg, now);
    }
    
    /**
     * 更新扩展字段。
     * 
     * @param key 扩展字段键
     * @param value 扩展字段值
     */
    private void updateExt(String key, Object value) {
        if (value == null) {
            return;
        }
        Map<String, Object> copy = new HashMap<>(this.ext == null ? Collections.emptyMap() : this.ext);
        copy.put(key, value);
        this.ext = copy;
    }
}
