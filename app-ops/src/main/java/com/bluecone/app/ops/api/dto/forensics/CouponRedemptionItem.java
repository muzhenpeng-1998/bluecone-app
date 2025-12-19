package com.bluecone.app.ops.api.dto.forensics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券核销记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRedemptionItem {
    
    /**
     * 核销记录ID
     */
    private Long id;
    
    /**
     * 优惠券ID
     */
    private Long couponId;
    
    /**
     * 模板ID
     */
    private Long templateId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 幂等键
     */
    private String idempotencyKey;
    
    /**
     * 原价
     */
    private BigDecimal originalAmount;
    
    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;
    
    /**
     * 最终金额
     */
    private BigDecimal finalAmount;
    
    /**
     * 核销时间
     */
    private LocalDateTime redemptionTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 模板名称
     */
    private String templateName;
    
    /**
     * 发放日志ID
     */
    private Long grantLogId;
    
    /**
     * 发放来源
     */
    private String grantSource;
    
    /**
     * 发放时间
     */
    private LocalDateTime grantTime;
}
