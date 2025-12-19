package com.bluecone.app.promo.domain.repository;

import com.bluecone.app.promo.domain.model.CouponRedemption;

import java.util.List;

/**
 * 优惠券核销记录领域仓储接口
 */
public interface CouponRedemptionRepository {

    /**
     * 根据幂等键查询核销记录
     * 
     * @param idempotencyKey 幂等键
     * @return 核销记录，不存在返回null
     */
    CouponRedemption findByIdempotencyKey(String idempotencyKey);

    /**
     * 根据订单ID查询核销记录
     */
    List<CouponRedemption> findByOrderId(Long tenantId, Long orderId);

    /**
     * 保存核销记录（幂等键唯一约束兜底）
     * 
     * @param redemption 核销记录
     */
    void save(CouponRedemption redemption);
}
