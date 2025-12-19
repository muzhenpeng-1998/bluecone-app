package com.bluecone.app.promo.api.facade;

import com.bluecone.app.promo.api.dto.CouponQueryContext;
import com.bluecone.app.promo.api.dto.UsableCouponDTO;

import java.util.List;

/**
 * 优惠券查询门面
 */
public interface CouponQueryFacade {
    
    /**
     * 查询用户可用优惠券列表
     * 
     * @param context 查询上下文
     * @return 可用优惠券列表（包含不可用原因）
     */
    List<UsableCouponDTO> listUsableCoupons(CouponQueryContext context);
    
    /**
     * 查询最优优惠券（优惠金额最大）
     * 
     * @param context 查询上下文
     * @return 最优优惠券，如无可用券返回null
     */
    UsableCouponDTO bestCoupon(CouponQueryContext context);
}
