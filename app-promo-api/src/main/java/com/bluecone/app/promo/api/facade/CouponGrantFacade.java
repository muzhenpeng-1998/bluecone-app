package com.bluecone.app.promo.api.facade;

import com.bluecone.app.promo.api.dto.CouponGrantCommand;
import com.bluecone.app.promo.api.dto.CouponGrantResult;

/**
 * 优惠券发放门面
 * 用于：
 * - 新用户注册发券
 * - 营销活动发券
 * - 管理员手动发券
 */
public interface CouponGrantFacade {
    
    /**
     * 发放优惠券（单个用户）
     * 
     * @param command 发券命令
     * @return 发券结果
     */
    CouponGrantResult grantCoupon(CouponGrantCommand command);
}
