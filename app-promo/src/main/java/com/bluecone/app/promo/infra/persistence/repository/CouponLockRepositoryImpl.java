package com.bluecone.app.promo.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.promo.domain.model.CouponLock;
import com.bluecone.app.promo.domain.repository.CouponLockRepository;
import com.bluecone.app.promo.infra.persistence.converter.CouponConverter;
import com.bluecone.app.promo.infra.persistence.mapper.CouponLockMapper;
import com.bluecone.app.promo.infra.persistence.po.CouponLockPO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

/**
 * 优惠券锁定记录仓储实现
 */
@Repository
@RequiredArgsConstructor
public class CouponLockRepositoryImpl implements CouponLockRepository {

    private final CouponLockMapper couponLockMapper;

    @Override
    public CouponLock findByIdempotencyKey(String idempotencyKey) {
        CouponLockPO po = couponLockMapper.selectOne(new LambdaQueryWrapper<CouponLockPO>()
                .eq(CouponLockPO::getIdempotencyKey, idempotencyKey));
        return CouponConverter.toDomain(po);
    }

    @Override
    public CouponLock findByOrderId(Long tenantId, Long orderId) {
        CouponLockPO po = couponLockMapper.selectOne(new LambdaQueryWrapper<CouponLockPO>()
                .eq(CouponLockPO::getTenantId, tenantId)
                .eq(CouponLockPO::getOrderId, orderId)
                .orderByDesc(CouponLockPO::getCreatedAt)
                .last("LIMIT 1"));
        return CouponConverter.toDomain(po);
    }

    @Override
    public void save(CouponLock lock) {
        if (lock == null) {
            return;
        }
        CouponLockPO po = CouponConverter.toPO(lock);
        try {
            couponLockMapper.insert(po);
        } catch (DuplicateKeyException e) {
            // 幂等键冲突，说明已经存在，忽略（由调用方根据查询结果判断）
            // 这里可以记录日志或者抛出自定义异常
        }
    }

    @Override
    public int update(CouponLock lock) {
        if (lock == null || lock.getId() == null) {
            return 0;
        }
        CouponLockPO po = CouponConverter.toPO(lock);
        return couponLockMapper.updateById(po);
    }
}
