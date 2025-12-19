package com.bluecone.app.wallet.domain.model;

import com.bluecone.app.wallet.domain.enums.RechargeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值单领域模型
 * 
 * @author bluecone
 * @since 2025-12-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeOrder {
    
    /**
     * 充值单ID（内部主键，ULID）
     */
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 账户ID
     */
    private Long accountId;
    
    /**
     * 充值单号（对外展示，PublicId格式：wrc_xxx）
     */
    private String rechargeNo;
    
    /**
     * 充值金额（单位：分）
     */
    private Long rechargeAmount;
    
    /**
     * 赠送金额（单位：分）
     */
    private Long bonusAmount;
    
    /**
     * 总到账金额（单位：分）= rechargeAmount + bonusAmount
     */
    private Long totalAmount;
    
    /**
     * 币种
     */
    private String currency;
    
    /**
     * 充值状态
     */
    private RechargeStatus status;
    
    /**
     * 支付单ID
     */
    private Long payOrderId;
    
    /**
     * 支付渠道（WECHAT、ALIPAY）
     */
    private String payChannel;
    
    /**
     * 第三方支付单号（渠道交易号）
     */
    private String channelTradeNo;
    
    /**
     * 充值发起时间
     */
    private LocalDateTime requestedAt;
    
    /**
     * 充值完成时间（支付成功时间）
     */
    private LocalDateTime paidAt;
    
    /**
     * 幂等键
     */
    private String idempotencyKey;
    
    /**
     * 扩展信息JSON
     */
    private String extJson;
    
    /**
     * 乐观锁版本号
     */
    private Integer version;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 创建人
     */
    private Long createdBy;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 更新人
     */
    private Long updatedBy;
    
    // ========== 业务方法 ==========
    
    /**
     * 转换为元（用于展示）
     */
    public BigDecimal getRechargeAmountInYuan() {
        return rechargeAmount == null ? BigDecimal.ZERO 
                : BigDecimal.valueOf(rechargeAmount).divide(BigDecimal.valueOf(100));
    }
    
    public BigDecimal getBonusAmountInYuan() {
        return bonusAmount == null ? BigDecimal.ZERO 
                : BigDecimal.valueOf(bonusAmount).divide(BigDecimal.valueOf(100));
    }
    
    public BigDecimal getTotalAmountInYuan() {
        return totalAmount == null ? BigDecimal.ZERO 
                : BigDecimal.valueOf(totalAmount).divide(BigDecimal.valueOf(100));
    }
    
    /**
     * 标记为支付中
     */
    public void markAsPaying(Long payOrderId, String payChannel) {
        if (!status.canTransitionTo(RechargeStatus.PAYING)) {
            throw new IllegalStateException(
                    String.format("充值单状态不允许流转：%s -> PAYING", status.getCode()));
        }
        this.status = RechargeStatus.PAYING;
        this.payOrderId = payOrderId;
        this.payChannel = payChannel;
    }
    
    /**
     * 标记为已支付
     */
    public void markAsPaid(String channelTradeNo, LocalDateTime paidAt) {
        if (!status.canTransitionTo(RechargeStatus.PAID)) {
            throw new IllegalStateException(
                    String.format("充值单状态不允许流转：%s -> PAID", status.getCode()));
        }
        this.status = RechargeStatus.PAID;
        this.channelTradeNo = channelTradeNo;
        this.paidAt = paidAt;
    }
    
    /**
     * 标记为已关闭
     */
    public void markAsClosed() {
        if (!status.canTransitionTo(RechargeStatus.CLOSED)) {
            throw new IllegalStateException(
                    String.format("充值单状态不允许流转：%s -> CLOSED", status.getCode()));
        }
        this.status = RechargeStatus.CLOSED;
    }
    
    /**
     * 是否为终态
     */
    public boolean isFinalState() {
        return status != null && status.isFinalState();
    }
}
