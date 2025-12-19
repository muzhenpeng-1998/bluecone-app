package com.bluecone.app.ops.api.dto.forensics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 优惠券锁定记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponLockItem {
    
    /**
     * 锁定记录ID
     */
    private Long id;
    
    /**
     * 优惠券ID
     */
    private Long couponId;
    
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
     * 锁定状态：LOCKED/COMMITTED/RELEASED
     */
    private String lockStatus;
    
    /**
     * 锁定时间
     */
    private LocalDateTime lockTime;
    
    /**
     * 释放时间
     */
    private LocalDateTime releaseTime;
    
    /**
     * 提交时间
     */
    private LocalDateTime commitTime;
    
    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 模板ID
     */
    private Long templateId;
    
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
}
