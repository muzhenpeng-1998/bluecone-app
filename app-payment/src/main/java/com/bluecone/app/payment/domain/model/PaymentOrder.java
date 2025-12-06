package com.bluecone.app.payment.domain.model;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.payment.domain.enums.PaymentChannel;
import com.bluecone.app.payment.domain.enums.PaymentMethod;
import com.bluecone.app.payment.domain.enums.PaymentScene;
import com.bluecone.app.payment.domain.enums.PaymentStatus;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付单聚合根：
 * - 承载支付领域的核心状态与金额一致性规则；
 * - 不做外部 IO/渠道调用，只在领域内校验和更新状态；
 * - 与订单域保持一致的风格与语义。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    // 标识与多租户
    private Long id;

    private Long tenantId;

    private Long storeId;

    // 业务主键（可选，若业务订单主键为 Long，可映射到此字段）
    private Long bizOrderId;

    // 业务关联信息
    private String bizType;

    private String bizOrderNo;

    private String paymentNo;

    // 用户
    private Long userId;

    // 渠道与方式
    private PaymentChannel channel;

    private PaymentMethod method;

    private PaymentScene scene;

    // 金额信息
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal payableAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Builder.Default
    private String currency = "CNY";

    // 状态相关
    private PaymentStatus status;

    private LocalDateTime expireAt;

    private LocalDateTime paidAt;

    // 渠道相关信息
    private String channelTradeNo;

    @Builder.Default
    private Map<String, Object> channelContext = Collections.emptyMap();

    // 业务扩展字段
    @Builder.Default
    private Map<String, Object> ext = Collections.emptyMap();

    // 幂等与安全
    private String idempotentKey;

    // 版本与审计字段
    @Builder.Default
    private Integer version = 0;

    private LocalDateTime createdAt;

    private Long createdBy;

    private LocalDateTime updatedAt;

    private Long updatedBy;

    /**
     * 初始化金额：归一化总金额与优惠金额，并计算应付金额。
     * 仅进行领域内的金额合法性校验，不做任何外部调用。
     *
     * @param totalAmount    原始金额（不可为负）
     * @param discountAmount 折扣金额（不可为负，且不超过 total）
     */
    public void initAmounts(BigDecimal totalAmount, BigDecimal discountAmount) {
        BigDecimal safeTotal = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        BigDecimal safeDiscount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        if (safeTotal.compareTo(BigDecimal.ZERO) < 0 || safeDiscount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "支付金额不合法");
        }
        BigDecimal payable = safeTotal.subtract(safeDiscount);
        if (payable.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "支付金额不合法");
        }
        this.totalAmount = safeTotal;
        this.discountAmount = safeDiscount;
        this.payableAmount = payable;
    }

    /**
     * 支付单创建初始化。
     */
    public void markInit() {
        this.status = PaymentStatus.INIT;
        this.paidAmount = BigDecimal.ZERO;
    }

    /**
     * 渠道预下单成功后标记为待支付。
     */
    public void markPending() {
        if (this.status != PaymentStatus.INIT) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "当前状态不允许进入待支付");
        }
        this.status = PaymentStatus.PENDING;
    }

    /**
     * 标记支付成功，校验金额与渠道交易号的一致性。
     *
     * @param paidAmount     实际支付金额
     * @param channelTradeNo 渠道交易号
     * @param paidAt         支付完成时间
     */
    public void markSuccess(BigDecimal paidAmount, String channelTradeNo, LocalDateTime paidAt) {
        BigDecimal safePaid = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        BigDecimal safePayable = this.payableAmount == null ? BigDecimal.ZERO : this.payableAmount;

        if (this.status == PaymentStatus.SUCCESS) {
            BigDecimal currentPaid = this.paidAmount == null ? BigDecimal.ZERO : this.paidAmount;
            String currentTradeNo = this.channelTradeNo;
            if (currentPaid.compareTo(safePaid) != 0 || (currentTradeNo != null && !currentTradeNo.equals(channelTradeNo))) {
                throw new BizException(CommonErrorCode.BAD_REQUEST, "支付回调与当前支付单不一致");
            }
            return;
        }

        if (this.status == PaymentStatus.PENDING || this.status == PaymentStatus.INIT) {
            if (safePaid.compareTo(BigDecimal.ZERO) < 0 || safePaid.compareTo(safePayable) > 0) {
                throw new BizException(CommonErrorCode.BAD_REQUEST, "支付金额不合法");
            }
            this.paidAmount = safePaid;
            this.channelTradeNo = channelTradeNo;
            this.paidAt = paidAt;
            this.status = PaymentStatus.SUCCESS;
            return;
        }

        if (this.status == PaymentStatus.CLOSED || this.status == PaymentStatus.FAILED || this.status == PaymentStatus.REFUNDED) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "支付单已终态，拒绝变更为成功");
        }

        throw new BizException(CommonErrorCode.BAD_REQUEST, "当前状态不支持支付成功回调");
    }

    /**
     * 标记支付失败（渠道失败或幂等处理），不做外部调用。
     */
    public void markFailed(String reason) {
        if (this.status == PaymentStatus.INIT || this.status == PaymentStatus.PENDING) {
            this.status = PaymentStatus.FAILED;
            return;
        }
        throw new BizException(CommonErrorCode.BAD_REQUEST, "当前状态不允许标记为失败");
    }

    /**
     * 标记为已取消（超时或主动取消），不处理已完成的终态。
     */
    public void markCanceled() {
        if (this.status == PaymentStatus.SUCCESS || this.status == PaymentStatus.REFUNDED) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "支付已完成，不能取消");
        }
        if (this.status == PaymentStatus.CANCELED) {
            return;
        }
        this.status = PaymentStatus.CANCELED;
    }

    /**
     * 标记为已关闭（超时或人工关闭），不会处理已成功或已退款的终态。
     */
    public void markClosed() {
        if (this.status == PaymentStatus.SUCCESS || this.status == PaymentStatus.REFUNDED) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "支付已完成，不能关闭");
        }
        this.status = PaymentStatus.CLOSED;
    }

    /**
     * 判断当前支付单是否已到达终态。
     */
    public boolean isTerminal() {
        return this.status == PaymentStatus.SUCCESS
                || this.status == PaymentStatus.CLOSED
                || this.status == PaymentStatus.REFUNDED;
    }
}
