package com.bluecone.app.promo.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.promo.domain.model.CouponRedemption;
import com.bluecone.app.promo.domain.repository.CouponRedemptionRepository;
import com.bluecone.app.promo.infra.persistence.converter.CouponConverter;
import com.bluecone.app.promo.infra.persistence.mapper.CouponRedemptionMapper;
import com.bluecone.app.promo.infra.persistence.po.CouponRedemptionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 优惠券核销记录仓储实现
 */
@Repository
@RequiredArgsConstructor
public class CouponRedemptionRepositoryImpl implements CouponRedemptionRepository {

    private final CouponRedemptionMapper couponRedemptionMapper;

    @Override
    public CouponRedemption findByIdempotencyKey(String idempotencyKey) {
        CouponRedemptionPO po = couponRedemptionMapper.selectOne(new LambdaQueryWrapper<CouponRedemptionPO>()
                .eq(CouponRedemptionPO::getIdempotencyKey, idempotencyKey));
        return CouponConverter.toDomain(po);
    }

    @Override
    public List<CouponRedemption> findByOrderId(Long tenantId, Long orderId) {
        List<CouponRedemptionPO> poList = couponRedemptionMapper.selectList(new LambdaQueryWrapper<CouponRedemptionPO>()
                .eq(CouponRedemptionPO::getTenantId, tenantId)
                .eq(CouponRedemptionPO::getOrderId, orderId));
        return poList.stream()
                .map(CouponConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void save(CouponRedemption redemption) {
        if (redemption == null) {
            return;
        }
        CouponRedemptionPO po = CouponConverter.toPO(redemption);
        try {
            couponRedemptionMapper.insert(po);
        } catch (DuplicateKeyException e) {
            // 幂等键冲突，说明已经存在，忽略（由调用方根据查询结果判断）
            // 这里可以记录日志或者抛出自定义异常
        }
    }
}
