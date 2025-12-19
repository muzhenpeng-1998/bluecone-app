package com.bluecone.app.order.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("bc_order")
public class OrderPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private Long userId;

    private String sessionId;

    private Integer sessionVersion;

    private String orderNo;

    private String clientOrderNo;

    private String orderSource;

    private String bizType;

    private String channel;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payableAmount;

    private String currency;

    private Long couponId;

    private String status;

    private String payStatus;

    private String orderRemark;

    private String extJson;

    @Version
    private Integer version;

    private LocalDateTime createdAt;

    private Long createdBy;

    private LocalDateTime updatedAt;

    private Long updatedBy;

    private Long acceptOperatorId;

    private LocalDateTime acceptedAt;

    /**
     * 商户拒单记录。
     */
    private String rejectReasonCode;

    private String rejectReasonDesc;

    private LocalDateTime rejectedAt;

    private Long rejectedBy;

    // TODO: 确保 bc_order 表已包含 user_deleted / user_deleted_at 字段
    private Boolean userDeleted;

    private LocalDateTime userDeletedAt;

    /**
     * 关单原因：PAY_TIMEOUT（支付超时）、USER_CANCEL（用户取消）、MERCHANT_CANCEL（商户取消）等。
     */
    private String closeReason;

    /**
     * 关单时间。
     */
    private LocalDateTime closedAt;

    /**
     * 开始制作时间（ACCEPTED → IN_PROGRESS）。
     */
    private LocalDateTime startedAt;

    /**
     * 出餐/可取时间（IN_PROGRESS → READY）。
     */
    private LocalDateTime readyAt;

    /**
     * 完成时间（READY → COMPLETED）。
     */
    private LocalDateTime completedAt;

    /**
     * 最近一次状态变化时间（用于SLA统计和超时判断）。
     */
    private LocalDateTime lastStateChangedAt;

    /**
     * 最近操作人ID（接单/开始/出餐/完成等操作的操作人）。
     */
    private Long operatorId;

    /**
     * 取消时间（用户取消或商户拒单时填充）。
     */
    private LocalDateTime canceledAt;

    /**
     * 取消原因码（USER_CANCEL、MERCHANT_REJECT、PAY_TIMEOUT等）。
     */
    private String cancelReasonCode;

    /**
     * 取消原因描述（用户或商户填写的具体原因）。
     */
    private String cancelReasonDesc;

    /**
     * 退款时间（退款成功时填充）。
     */
    private LocalDateTime refundedAt;

    /**
     * 退款单ID（关联bc_refund_order.id，可选）。
     */
    private Long refundOrderId;
}
