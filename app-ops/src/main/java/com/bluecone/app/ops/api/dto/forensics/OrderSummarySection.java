package com.bluecone.app.ops.api.dto.forensics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单摘要信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummarySection {
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 门店ID
     */
    private Long storeId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 客户端订单号
     */
    private String clientOrderNo;
    
    /**
     * 订单状态
     */
    private String status;
    
    /**
     * 支付状态
     */
    private String payStatus;
    
    /**
     * 业务类型
     */
    private String bizType;
    
    /**
     * 订单来源
     */
    private String orderSource;
    
    /**
     * 渠道
     */
    private String channel;
    
    /**
     * 总金额
     */
    private BigDecimal totalAmount;
    
    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;
    
    /**
     * 应付金额
     */
    private BigDecimal payableAmount;
    
    /**
     * 币种
     */
    private String currency;
    
    /**
     * 使用的优惠券ID
     */
    private Long couponId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 接单时间
     */
    private LocalDateTime acceptedAt;
    
    /**
     * 拒单时间
     */
    private LocalDateTime rejectedAt;
    
    /**
     * 关单时间
     */
    private LocalDateTime closedAt;
    
    /**
     * 关单原因
     */
    private String closeReason;
    
    /**
     * 拒单原因代码
     */
    private String rejectReasonCode;
    
    /**
     * 拒单原因描述
     */
    private String rejectReasonDesc;
    
    /**
     * 备注
     */
    private String remark;
}
