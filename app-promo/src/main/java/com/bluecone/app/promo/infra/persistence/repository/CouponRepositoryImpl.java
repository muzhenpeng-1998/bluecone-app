package com.bluecone.app.promo.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.promo.api.enums.CouponStatus;
import com.bluecone.app.promo.domain.model.Coupon;
import com.bluecone.app.promo.domain.repository.CouponRepository;
import com.bluecone.app.promo.infra.persistence.converter.CouponConverter;
import com.bluecone.app.promo.infra.persistence.mapper.CouponMapper;
import com.bluecone.app.promo.infra.persistence.po.CouponPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 优惠券仓储实现
 */
@Repository("promoCouponRepositoryImpl")
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponMapper couponMapper;

    @Override
    public Coupon findById(Long tenantId, Long couponId) {
        CouponPO po = couponMapper.selectOne(new LambdaQueryWrapper<CouponPO>()
                .eq(CouponPO::getTenantId, tenantId)
                .eq(CouponPO::getId, couponId));
        return CouponConverter.toDomain(po);
    }

    @Override
    public Coupon findByCouponCode(String couponCode) {
        CouponPO po = couponMapper.selectOne(new LambdaQueryWrapper<CouponPO>()
                .eq(CouponPO::getCouponCode, couponCode));
        return CouponConverter.toDomain(po);
    }

    @Override
    public List<Coupon> findUserCoupons(Long tenantId, Long userId, List<CouponStatus> statusList, LocalDateTime validTime) {
        LambdaQueryWrapper<CouponPO> wrapper = new LambdaQueryWrapper<CouponPO>()
                .eq(CouponPO::getTenantId, tenantId)
                .eq(CouponPO::getUserId, userId);
        
        // 状态过滤
        if (statusList != null && !statusList.isEmpty()) {
            List<String> statusCodes = statusList.stream()
                    .map(Enum::name)
                    .collect(Collectors.toList());
            wrapper.in(CouponPO::getStatus, statusCodes);
        }
        
        // 有效期过滤
        if (validTime != null) {
            wrapper.le(CouponPO::getValidStartTime, validTime)
                    .ge(CouponPO::getValidEndTime, validTime);
        }
        
        wrapper.orderByDesc(CouponPO::getCreatedAt);
        
        List<CouponPO> poList = couponMapper.selectList(wrapper);
        return poList.stream()
                .map(CouponConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void save(Coupon coupon) {
        if (coupon == null) {
            return;
        }
        CouponPO po = CouponConverter.toPO(coupon);
        couponMapper.insert(po);
    }

    @Override
    public int update(Coupon coupon) {
        if (coupon == null || coupon.getId() == null) {
            return 0;
        }
        CouponPO po = CouponConverter.toPO(coupon);
        return couponMapper.updateById(po);
    }

    @Override
    public int updateStatus(Long couponId, CouponStatus expectedStatus, CouponStatus newStatus, 
                           Long orderId, LocalDateTime lockTime, LocalDateTime useTime) {
        if (couponId == null || expectedStatus == null || newStatus == null) {
            return 0;
        }
        
        LambdaUpdateWrapper<CouponPO> wrapper = new LambdaUpdateWrapper<CouponPO>()
                .eq(CouponPO::getId, couponId)
                .eq(CouponPO::getStatus, expectedStatus.name())
                .set(CouponPO::getStatus, newStatus.name());
        
        if (orderId != null) {
            wrapper.set(CouponPO::getOrderId, orderId);
        }
        if (lockTime != null) {
            wrapper.set(CouponPO::getLockTime, lockTime);
        }
        if (useTime != null) {
            wrapper.set(CouponPO::getUseTime, useTime);
        }
        
        return couponMapper.update(null, wrapper);
    }

    @Override
    public List<Coupon> findByIds(Long tenantId, List<Long> couponIds) {
        if (couponIds == null || couponIds.isEmpty()) {
            return List.of();
        }
        
        List<CouponPO> poList = couponMapper.selectList(new LambdaQueryWrapper<CouponPO>()
                .eq(CouponPO::getTenantId, tenantId)
                .in(CouponPO::getId, couponIds));
        
        return poList.stream()
                .map(CouponConverter::toDomain)
                .collect(Collectors.toList());
    }
}
