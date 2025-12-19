package com.bluecone.app.ops.api.dto.forensics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 优惠券操作汇总
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponSection {
    
    /**
     * 优惠券锁定记录列表
     */
    private List<CouponLockItem> locks;
    
    /**
     * 优惠券核销记录列表
     */
    private List<CouponRedemptionItem> redemptions;
    
    /**
     * 记录总数（用于判断是否被截断）
     */
    private Integer totalCount;
}
