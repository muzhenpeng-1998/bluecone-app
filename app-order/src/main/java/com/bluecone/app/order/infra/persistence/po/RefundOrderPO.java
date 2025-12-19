package com.bluecone.app.order.infra.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款单持久化对象（对应表 bc_refund_order）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundOrderPO {
    
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
    
    /**
     * 退款单号（对外展示，PublicId格式：rfd_xxx）。
     */
    private String refundId;
    
    /**
     * 退款渠道（WECHAT、ALIPAY、MOCK）。
     */
    private String channel;
    
    /**
     * 退款金额（实际退款金额，单位：元）。
     */
    private BigDecimal refundAmount;
    
    /**
     * 币种（默认：CNY）。
     */
    private String currency;
    
    /**
     * 退款状态（INIT、PROCESSING、SUCCESS、FAILED）。
     */
    private String status;
    
    /**
     * 第三方退款单号（如微信退款单号，退款成功后由回调填充）。
     */
    private String refundNo;
    
    /**
     * 退款原因码（USER_CANCEL、MERCHANT_REJECT、OUT_OF_STOCK等）。
     */
    private String reasonCode;
    
    /**
     * 退款原因描述（用户或商户填写的具体原因）。
     */
    private String reasonDesc;
    
    /**
     * 幂等键（格式：{tenantId}:{storeId}:{orderId}:refund:{requestId}）。
     */
    private String idemKey;
    
    /**
     * 支付单ID（关联bc_payment_order.id）。
     */
    private Long payOrderId;
    
    /**
     * 第三方支付单号（如微信transaction_id，用于退款时传给支付网关）。
     */
    private String payNo;
    
    /**
     * 退款发起时间。
     */
    private LocalDateTime refundRequestedAt;
    
    /**
     * 退款完成时间（SUCCESS时填充）。
     */
    private LocalDateTime refundCompletedAt;
    
    /**
     * 扩展信息JSON（存储第三方退款响应等）。
     */
    private String extJson;
    
    /**
     * 乐观锁版本号。
     */
    private Integer version;
    
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
}
