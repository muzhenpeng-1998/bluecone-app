package com.bluecone.app.promo.domain.repository;

import com.bluecone.app.promo.domain.model.CouponLock;

/**
 * 优惠券锁定记录领域仓储接口
 */
public interface CouponLockRepository {

    /**
     * 根据幂等键查询锁定记录
     * 
     * @param idempotencyKey 幂等键
     * @return 锁定记录，不存在返回null
     */
    CouponLock findByIdempotencyKey(String idempotencyKey);

    /**
     * 根据订单ID查询锁定记录
     */
    CouponLock findByOrderId(Long tenantId, Long orderId);

    /**
     * 保存锁定记录（幂等键唯一约束兜底）
     * 
     * @param lock 锁定记录
     */
    void save(CouponLock lock);

    /**
     * 更新锁定记录
     * 
     * @return 更新的行数
     */
    int update(CouponLock lock);
}
